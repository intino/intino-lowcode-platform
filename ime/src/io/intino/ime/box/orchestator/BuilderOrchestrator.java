package io.intino.ime.box.orchestator;

import io.intino.alexandria.Resource;
import io.intino.alexandria.exceptions.InternalServerError;
import io.intino.alexandria.exceptions.NotFound;
import io.intino.alexandria.logger.Logger;
import io.intino.builderservice.QuassarBuilderServiceAccessor;
import io.intino.builderservice.schemas.Message;
import io.intino.builderservice.schemas.OperationResult;
import io.intino.builderservice.schemas.RunOperationContext;
import io.intino.ime.box.scaffolds.Scaffold;
import io.intino.ime.box.scaffolds.ScaffoldFactory;
import io.intino.ls.document.DocumentManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.intino.builderservice.schemas.OperationResult.State.Running;

public class BuilderOrchestrator {

	public static final String QUASSAR_FILE = "quassar";
	private final DocumentManager manager;
	private final QuassarBuilderServiceAccessor accessor;
	private final QuassarParser quassar;

	public BuilderOrchestrator(URL builderedServiceUrl, DocumentManager manager) {
		this.manager = manager;
		this.accessor = new QuassarBuilderServiceAccessor(builderedServiceUrl);
		this.quassar = new QuassarParser(quassarContent());
	}

	private String quassarContent() {
		try {
			return new String(manager.getDocumentText(new URI(QUASSAR_FILE)).readAllBytes());
		} catch (Exception e) {
			Logger.error(e);
			return "";
		}
	}

	public List<Message> exec(String user, String builder, String operation) {
		try {
			File taraFiles = taraFiles();
			if (taraFiles == null)
				return List.of(new Message().kind(Message.Kind.ERROR).content("Model files not found"));
			List<Message> messages = runBuild(builder, taraFiles, operation);
			if (messages.stream().anyMatch(m -> m.kind().equals(Message.Kind.ERROR))) return messages;
			manager.commit(user);
			manager.push();
			return messages;
		} catch (Throwable t) {
			Logger.error(t);
			return List.of(new Message().kind(Message.Kind.ERROR).content("Unknown error"));
		}
	}


	public List<Message> build(String user) {
		try {
			File taraFiles = taraFiles();
			if (taraFiles == null)
				return List.of(new Message().kind(Message.Kind.ERROR).content("Model files not found"));
			List<Message> messages = doBuild(taraFiles);
			if (!messages.isEmpty()) return messages;
			manager.commit(user);
			manager.push();
		} catch (Throwable t) {
			Logger.error(t);
			return List.of(new Message().kind(Message.Kind.ERROR).content("Unknown error"));
		}
		return Collections.emptyList();
	}

	private List<Message> doBuild(File taraFiles) throws InternalServerError, IOException, NotFound, InterruptedException, URISyntaxException {
		List<Message> messages = runBuild(quassar.tara(), taraFiles, "Build");
		if (!messages.isEmpty()) return messages;
		for (String b : builders()) {
			messages = runBuild(b.trim(), taraFiles, "Build");
			if (!messages.isEmpty()) return messages;
		}
		return Collections.emptyList();
	}

	private List<Message> runBuild(String builder, File taraFiles, String operation) throws InternalServerError, IOException, NotFound, InterruptedException, URISyntaxException {
		String ticket = accessor.postRunOperation(context(builder).operation(operation), Resource.InputStreamProvider.of(taraFiles));
		OperationResult output = accessor.getOperationOutput(ticket);
		while (output.state() == Running) {
			Thread.sleep(1000);
			output = accessor.getOperationOutput(ticket);
		}
		if (!output.success()) return output.messages();
		doExtraction(ticket, output, quassar.pathOf(builder), scaffoldOf(builder));
		//moveGraphJson();
		return Collections.emptyList();
	}

	private Scaffold scaffoldOf(String builder) {
		ArchetypeEntry entry = quassar.paths().stream()
				.filter(e -> e.builders().stream().anyMatch(b -> b.contains(builder)))
				.findFirst()
				.orElse(null);
		return entry != null && entry.scaffold() != null ? ScaffoldFactory.scaffoldOf(ScaffoldFactory.Scaffold.valueOf(entry.scaffold()), manager, "") : null;
	}

	private void doExtraction(String ticket, OperationResult output, String builderPath, Scaffold scaffold) throws InternalServerError, NotFound, IOException {
		extractFiles(ticket, output.genRef(), builderPath + "/" + (scaffold != null ? scaffold.genPath() : "gen"), true);
		extractFiles(ticket, output.srcRef(), builderPath + "/" + (scaffold != null ? scaffold.srcPath() : "src"), false);
		extractFiles(ticket, output.outRef(), builderPath + "/" + (scaffold != null ? scaffold.outPath() : "out"), true);
		extractFiles(ticket, output.resRef(), builderPath + "/" + (scaffold != null ? scaffold.resPath() : "res"), true);
	}

	private void moveGraphJson() throws URISyntaxException {
		URI old = manager.all().stream().filter(u -> u.getPath().endsWith("graph.json")).findFirst().orElse(null);
		if (old != null && !old.getPath().endsWith("graph/graph.json"))
			manager.move(old, new URI("graph/graph.json"));
	}

	private void extractFiles(String ticket, String ref, String path, boolean replace) throws InternalServerError, NotFound, IOException {
		if (ref == null || ref.isEmpty()) return;
		TarUtils.decompressTarFile(accessor.getOutputResource(ticket, ref, ".*\\." + langExtension() + "$").bytes(), manager, path, replace);
	}

	private List<String> builders() {
		return Arrays.asList(quassar.valueOf("builders").split(","));
	}

	private File taraFiles() {
		try {
			List<URI> uris = modelUris();
			if (uris.isEmpty()) return null;
			return TarUtils.createTarFile(manager, uris, Files.createTempFile("quassar", ".tar").toFile());
		} catch (IOException e) {
			Logger.error(e);
			return null;
		}
	}

	private List<URI> modelUris() {
		return manager.all().stream().filter(l -> l.getPath().endsWith("." + langExtension())).toList();
	}

	private String langExtension() {
		return quassar.langName().toLowerCase();
	}

	private RunOperationContext context(String builder) {
		return new RunOperationContext()
				.imageURL(builder)
				.generationPackage(quassar.codePackage())
				.language(quassar.langQn())
				.languageVersion(quassar.langVersion())
				.project(quassar.projectName())
				.projectVersion(quassar.projectVersion());
	}

}