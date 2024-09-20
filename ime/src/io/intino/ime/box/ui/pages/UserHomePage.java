package io.intino.ime.box.ui.pages;

import io.intino.ime.box.ui.displays.templates.*;

public class UserHomePage extends AbstractUserHomePage {

	public io.intino.alexandria.ui.Soul prepareSoul(io.intino.alexandria.ui.services.push.UIClient client) {
		return new io.intino.alexandria.ui.Soul(session) {
			@Override
			public void personify() {
				UserHomeTemplate component = new UserHomeTemplate(box);
				register(component);
				component.init();
			}
		};
	}
}