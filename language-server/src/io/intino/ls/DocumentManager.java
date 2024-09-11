package io.intino.ls;

import io.intino.alexandria.logger.Logger;
import org.eclipse.lsp4j.TextDocumentItem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DocumentManager {
	private final Object lock = new Object();
	private final Map<URI, TextDocumentItem> documents;
	private final File root;

	public DocumentManager(File root) throws IOException {
		this.root = root;
		this.documents = loadDocuments(root);
	}

	public File root() {
		return root;
	}

	private TextDocumentItem documentItem(File f) {
		return new TextDocumentItem(relativePath(f).getPath(), dslOf(f), (int) f.lastModified(), content(f));
	}

	public List<URI> all() {
		return documents.keySet().stream().toList();
	}

	public void upsertDocument(URI uri, String dsl, String content) {
		synchronized (lock) {
			try {
				content = content.isEmpty() ? "dsl " + dsl + "\n\n" : content;
				File file = new File(root, uri.getPath());
				documents.put(uri, new TextDocumentItem(uri.toString(), dsl, (int) Instant.now().toEpochMilli(), content));
				file.getParentFile().mkdirs();
				Files.writeString(file.toPath(), content);
			} catch (IOException e) {
				Logger.error(e);
			}
		}
	}

	public InputStream getDocumentText(URI uri) {
		synchronized (lock) {
			TextDocumentItem document = documents.get(uri);
			return document != null ? new ByteArrayInputStream(document.getText().getBytes()) : null;
		}
	}

	public void removeDocument(URI uri) {
		documents.remove(uri);
	}

	private String content(File f) {
		try {
			return Files.readString(f.toPath());
		} catch (IOException e) {
			Logger.error(e);
			return "";
		}
	}

	private String dslOf(File f) {
		try {
			if (!f.exists()) return "";
			return Files.lines(f.toPath()).filter(l -> !l.trim().isEmpty()).findFirst().orElse("");
		} catch (IOException e) {
			Logger.error(e);
			return "";
		}
	}

	private Map<URI, TextDocumentItem> loadDocuments(File projectRoot) throws IOException {
		return Files.walk(projectRoot.toPath())
				.filter(p -> p.toFile().isFile() && p.toFile().getName().endsWith(".tara"))
				.map(Path::toFile)
				.collect(Collectors.toMap(this::relativePath, this::documentItem, (a, b) -> a, ConcurrentHashMap::new));
	}

	private URI relativePath(File f) {
		return URI.create(root.toPath().relativize(f.toPath()).toFile().getPath());
	}
}