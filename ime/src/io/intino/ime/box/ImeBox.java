package io.intino.ime.box;

import io.intino.alexandria.logger.Logger;
import io.intino.alexandria.ui.services.auth.Space;
import io.intino.amidas.accessor.alexandria.core.AmidasOauthAccessor;
import io.intino.ime.box.commands.Commands;
import io.intino.ime.box.commands.CommandsFactory;
import io.intino.ime.box.lsp.LanguageServerWebSocketHandler;
import io.intino.ime.box.util.Languages;
import io.intino.ime.box.util.WorkspaceSequence;
import io.intino.ime.box.workspaces.WorkspaceManager;
import io.intino.languagearchetype.Archetype;

import java.net.MalformedURLException;
import java.net.URL;

public class ImeBox extends AbstractBox {
	private Archetype archetype;
	private WorkspaceManager workspaceManager;
	private CommandsFactory commandsFactory;
	private LanguageProvider languageProvider;
	private AmidasOauthAccessor amidasOauthAccessor;

	public ImeBox(String[] args) {
		this(new ImeConfiguration(args));
		this.archetype = new Archetype(configuration.home());
	}

	public ImeBox(ImeConfiguration configuration) {
		super(configuration);
		this.archetype = new Archetype(configuration.home());
	}

	@Override
	public io.intino.alexandria.core.Box put(Object o) {
		super.put(o);
		return this;
	}

	public void beforeStart() {
		languageProvider = new LanguageProvider(archetype.repository().languages().root(), url(configuration.languageArtifactory()));
		commandsFactory = new CommandsFactory(this);
		workspaceManager = new WorkspaceManager(archetype);
		Languages.init(archetype.configuration().languages());
		WorkspaceSequence.init(archetype.configuration().workspaceSequence());
	}

	@Override
	protected void beforeSetupImeElementsUi(io.intino.alexandria.ui.UISpark sparkInstance) {
		LanguageServerWebSocketHandler handler = new LanguageServerWebSocketHandler(new LanguageServerFactory(languageProvider), workspaceManager);
		sparkInstance.service().webSocket("/dsl/tara", handler);
	}

	public void afterStart() {
	}

	public void beforeStop() {
	}

	public void afterStop() {
	}

	public LanguageProvider languageProvider() {
		return this.languageProvider;
	}

	public Archetype archetype() {
		return archetype;
	}

	public WorkspaceManager workspaceManager() {
		return workspaceManager;
	}

	public <F extends Commands> F commands(Class<F> clazz) {
		return commandsFactory.command(clazz);
	}

	protected io.intino.alexandria.ui.services.AuthService authService(java.net.URL authServiceUrl) {
		if (authServiceUrl == null) return null;
		if (amidasOauthAccessor == null) amidasOauthAccessor = new AmidasOauthAccessor(new Space(url(configuration().url())).name("quasar-ime"), authServiceUrl);
		return amidasOauthAccessor;
	}
}