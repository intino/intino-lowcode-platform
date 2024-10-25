import io.quassar.QuassarBuilderRunner;

public class QuassarcRunner {
	public static void main(String[] args) {
		QuassarBuilderRunner runner = new QuassarBuilderRunner();
		runner.register(GenerateGraphOperation.class);
		runner.register(GenerateModelAccessorOperation.class);
		runner.start(args);
	}
}
