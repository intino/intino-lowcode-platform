package io.intino.ime.box.ui.datasources;

import io.intino.alexandria.ui.model.datasource.Filter;
import io.intino.alexandria.ui.model.datasource.Group;
import io.intino.alexandria.ui.model.datasource.PageDatasource;
import io.intino.alexandria.ui.services.push.UISession;
import io.intino.ime.box.ImeBox;
import io.intino.ime.box.models.ModelManager;
import io.intino.ime.model.Language;
import io.intino.ime.model.Model;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ModelsDatasource extends PageDatasource<Model> {
	protected final ImeBox box;
	protected final UISession session;
	private final Boolean onlyPrivate;
	private String condition;
	private List<Filter> filters;
	private Sorting sorting;

	public ModelsDatasource(ImeBox box, UISession session, Boolean onlyPrivate) {
		this.box = box;
		this.session = session;
		this.onlyPrivate = onlyPrivate;
	}

	public enum Sorting { MostUsed, MostRecent }
	public void sort(Sorting sorting) {
		this.sorting = sorting;
	}

	public long itemCount() {
		return itemCount(condition, filters);
	}

	@Override
	public List<Model> items(int start, int count, String condition, List<Filter> filters, List<String> sortings) {
		saveParameters(condition, filters);
		List<Model> result = sort(load(condition, filters), sortings);
		int from = Math.min(start, result.size());
		int end = Math.min(start + count, result.size());
		return result.subList(from, end);
	}

	@Override
	public long itemCount(String condition, List<Filter> filters) {
		return load(condition, filters).size();
	}

	@Override
	public List<Group> groups(String key) {
		if (key.equalsIgnoreCase(DatasourceHelper.Owner)) return load().stream().map(Model::owner).distinct().map(o -> new Group().name(o).label(o)).toList();
		return new ArrayList<>();
	}

	protected List<Model> load() {
		ModelManager manager = box.modelManager();
		String username = username();
		if (onlyPrivate == null) return manager.ownerModels(username);
		return onlyPrivate ? manager.privateModels(username) : manager.publicModels(username);
	}

	protected String username() {
		return session.user() != null ? session.user().username() : Model.DefaultOwner;
	}

	private List<Model> load(String condition, List<Filter> filters) {
		List<Model> models = load();
		models = filterOwner(models, filters);
		models = filterCondition(models, condition);
		return models;
	}

	private List<Model> filterOwner(List<Model> models, List<Filter> filters) {
		List<String> owners = DatasourceHelper.categories(DatasourceHelper.Owner, filters);
		if (owners.isEmpty()) return models;
		return models.stream().filter(l -> owners.contains(l.owner())).collect(toList());
	}

	private List<Model> filterCondition(List<Model> models, String condition) {
		if (condition == null || condition.isEmpty()) return models;
		String[] conditions = condition.toLowerCase().split(" ");
		return models.stream().filter(w ->
				DatasourceHelper.matches(w.id(), conditions) ||
				DatasourceHelper.matches(w.label(), conditions) ||
				DatasourceHelper.matches(w.owner(), conditions) ||
				DatasourceHelper.matches(w.modelingLanguage(), conditions) ||
				DatasourceHelper.matches(w.releasedLanguage(), conditions)
		).collect(toList());
	}

	private List<Model> sort(List<Model> models, List<String> sortings) {
		if (sortings.contains("Language")) return models.stream().sorted(Comparator.comparing(m -> m.modelingLanguage() != null ? m.modelingLanguage() : "z")).toList();
		else if (sortings.contains("Owner")) return models.stream().sorted(Comparator.comparing(m -> m.owner() != null ? m.owner() : "z")).toList();
		return models.stream().sorted(Comparator.comparing(m -> m.label().toLowerCase())).toList();
	}

	private void saveParameters(String condition, List<Filter> filters) {
		this.condition = condition;
		this.filters = filters;
	}

}
