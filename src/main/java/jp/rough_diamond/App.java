package jp.rough_diamond;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import jp.rough_diamond.tools.redmine.IssueRepository;
import jp.rough_diamond.tools.redmine.OGNLFilter;
import jp.rough_diamond.tools.redmine.OGNLFlatter;
import jp.rough_diamond.tools.redmine.OGNLSorter;
import ognl.OgnlException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.orangesignal.csv.CsvConfig;
import com.orangesignal.csv.CsvWriter;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager.INCLUDE;
import com.taskadapter.redmineapi.bean.Issue;

public class App {
	static Configuration config;
	public static void main(String[] args) throws ConfigurationException, RedmineException, OgnlException, IOException {
		makeConfig();

		IssueRepository repo = getRepository();
		
		Iterable<Issue> issues = getIssues(repo);

		OGNLFlatter<Issue> flatter = getFlatter();
		
		Writer writer = new OutputStreamWriter(System.out);
		CsvConfig config = new CsvConfig(',', '\"', '\"');
		try (CsvWriter csvWriter = new CsvWriter(writer, config)) {
			List<String> header = Arrays.asList(flatter.getHeader());
			System.err.println(header);
			csvWriter.writeValues(header);
			for(Object[] row : Iterables.transform(issues, flatter)) {
				List<String> rowStr = Lists.newArrayList(Iterables.transform(Arrays.asList(row), new Function<Object, String>() {
					@Override
					public String apply(Object o) {
						return (o == null) ? null : o.toString();
					}
				}));
				csvWriter.writeValues(rowStr);
			}
		}
	}

	private static OGNLFlatter<Issue> getFlatter() throws OgnlException {
		String flatter = getFlatterText();
		System.err.println(flatter);
		return new OGNLFlatter<Issue>(flatter);
	}

	private static String getFlatterText() {
		String[] flatters = config.getStringArray("flatter");
		String flatter = Joiner.on(',').join(flatters);
		return flatter;
	}

	private static Iterable<Issue> getIssues(IssueRepository repo)
			throws RedmineException, OgnlException {
		String queryIdStr = config.getString("redmine.queryId", "");
		Integer queryId = (queryIdStr.isEmpty()) ? null : Integer.parseInt(queryIdStr);
		System.err.println(queryId);
		String[] includeArray = config.getStringArray("redmine.includes");
		System.err.println(includeArray.length);
		INCLUDE[] includes = Iterables.toArray(Iterables.transform(Lists.newArrayList(includeArray), new Function<String, INCLUDE>() {
			@Override
			public INCLUDE apply(String name) {
				System.err.println(name);
				return INCLUDE.valueOf(name);
			}
		}), INCLUDE.class);
		String localFilter = config.getString("localFilterRef", "").trim();
		System.err.println(localFilter);
		Predicate<Issue> filter = (localFilter.isEmpty()) ? Predicates.<Issue>alwaysTrue() : new OGNLFilter<Issue>(localFilter);
		System.err.println(filter);
		Iterable<Issue> issues = repo.byQueryId(queryId, filter, includes);

		String sorterText = config.getString("sorterRef", "").trim();
		if(sorterText.isEmpty()) {
			return issues;
		} else {
			OGNLSorter<Issue> sorter = new OGNLSorter<>(sorterText);
			return Ordering.<Issue>from(sorter).sortedCopy(issues);
		}
	}

	private static IssueRepository getRepository() {
		String host = config.getString("redmine.url");
		String accessKey = config.getString("redmine.accessKey");
		String projectName = config.getString("redmine.projectKey");
		
		System.err.println(host);
		System.err.println(accessKey);
		System.err.println(projectName);
		
		IssueRepository repo = IssueRepository.getRepository(host, projectName, accessKey);
		return repo;
	}
	private static void makeConfig() throws ConfigurationException {
		URL url = App.class.getResource("/profile.properties");
		config = new PropertiesConfiguration(url);
	}
}
