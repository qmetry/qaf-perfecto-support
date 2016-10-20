/*******************************************************************************
 * QMetry Automation Framework provides a powerful and versatile platform to
 * author
 * Automated Test Cases in Behavior Driven, Keyword Driven or Code Driven
 * approach
 * Copyright 2016 Infostretch Corporation
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 * You should have received a copy of the GNU General Public License along with
 * this program in the name of LICENSE.txt in the root folder of the
 * distribution. If not, see https://opensource.org/licenses/gpl-3.0.html
 * See the NOTICE.TXT file in root folder of this source files distribution
 * for additional information regarding copyright ownership and licenses
 * of other open source software / files used by QMetry Automation Framework.
 * For any inquiry or need additional information, please contact
 * support-qaf@infostretch.com
 *******************************************************************************/

package com.qmetry.qaf.automation.support.perfecto;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.util.List;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResultFactory;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.step.QAFTestStepAdapter;
import com.qmetry.qaf.automation.step.QAFTestStepListener;
import com.qmetry.qaf.automation.step.StepExecutionTracker;
import com.qmetry.qaf.automation.step.client.TestNGScenario;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;

/**
 * This class transmits test results including each test step directly to
 * Reportium server. You need to be registered this class as TestNG listener and
 * optionally
 * as {@link QAFTestStepListener}.
 * <p>
 * This class also allows to transmits additional information during test
 * execution using
 * {@link #logTestStep(String)}.
 * 
 * @author chirag.jayswal
 */
public class PerfectoReportiumConnector extends QAFTestStepAdapter
		implements
			ITestListener {
	private static final String PERFECTO_REPORT_CLIENT = "perfecto.report.client";

	@Override
	public void onTestStart(ITestResult testResult) {
		if (getBundle().getString("remote.server", "").contains("perfecto")) {

			ReportiumClient reportClient = getReportiumClient(testResult);
			TestContext context = new TestContext(testResult.getMethod().getGroups());

			reportClient.testStart(testResult.getMethod().getMethodName(), context);
			addReportLink(testResult, reportClient.getReportUrl());
		}
	}

	@Override
	public void beforExecute(StepExecutionTracker stepExecutionTracker) {
		logTestStep(stepExecutionTracker.getStep().getDescription());
	}

	@Override
	public void onTestSuccess(ITestResult testResult) {
		ReportiumClient client = getReportiumClient();
		if (null != client)
			client.testStop(TestResultFactory.createSuccess());
	}

	@Override
	public void onTestFailure(ITestResult testResult) {
		ReportiumClient client = getReportiumClient();
		if (null != client)
			client.testStop(TestResultFactory.createFailure("An error occurred",
					testResult.getThrowable()));
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		// do nothing
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		onTestFailure(result);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onStart(ITestContext context) {
		if (getBundle().getString("remote.server", "").contains("perfecto")) {

			List<String> stepListeners =
					getBundle().getList(ApplicationProperties.TESTSTEP_LISTENERS.key);
			if (!stepListeners.contains(this.getClass().getName())) {
				stepListeners.add(this.getClass().getName());
				getBundle().setProperty(ApplicationProperties.TESTSTEP_LISTENERS.key,
						stepListeners);
			}

			if (getBundle().getBoolean("perfecto.default.driver.listener", true)) {
				List<String> driverListeners = getBundle()
						.getList(ApplicationProperties.WEBDRIVER_COMMAND_LISTENERS.key);
				if (!driverListeners.contains(PerfectoDriverListener.class.getName())) {
					driverListeners.add(PerfectoDriverListener.class.getName());
					getBundle().setProperty(
							ApplicationProperties.WEBDRIVER_COMMAND_LISTENERS.key,
							driverListeners);
				}
			}
		}
	}

	@Override
	public void onFinish(ITestContext context) {
		// do nothing
	}

	public static void logTestStep(String message) {
		try {
			getReportiumClient().testStep(message);
		} catch (Exception e) {
			// ignore...
		}
	}

	private static ReportiumClient getReportiumClient() {
		return (ReportiumClient) getBundle().getObject(PERFECTO_REPORT_CLIENT);
	}

	/**
	 * Creates client and set into configuration for later use during test
	 * execution using {@link #getReportiumClient()}.
	 * 
	 * @param result
	 * @return newly created {@link ReportiumClient} object
	 */
	private ReportiumClient getReportiumClient(ITestResult result) {
		String suiteName = result.getTestContext().getSuite().getName();
		String prjName = getBundle().getString("project.name", suiteName);
		String prjVer = getBundle().getString("project.ver", "1.0");
		String xmlTestName = result.getTestContext().getName();

		QAFExtendedWebDriver driver = new WebDriverTestBase().getDriver();

		PerfectoExecutionContext perfectoExecutionContext =
				new PerfectoExecutionContext.PerfectoExecutionContextBuilder()
						.withProject(new Project(prjName, prjVer))
						.withContextTags(suiteName, xmlTestName)
						.withJob(new Job(getBundle().getString("JOB_NAME"),
								getBundle().getInt("BUILD_NUMBER", 0)))
						.withWebDriver(driver).build();

		ReportiumClient reportClient = new ReportiumClientFactory()
				.createPerfectoReportiumClient(perfectoExecutionContext);
		getBundle().setProperty(PERFECTO_REPORT_CLIENT, reportClient);

		return reportClient;
	}

	private void addReportLink(ITestResult result, String url) {
		((TestNGScenario) result.getMethod()).getMetaData().put("Perfecto-report",
				"<a href=\"" + url + "\" target=\"_blank\">view</a>");
	}

}
