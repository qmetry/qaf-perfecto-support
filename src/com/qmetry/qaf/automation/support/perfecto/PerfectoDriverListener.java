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
import static com.qmetry.qaf.automation.support.perfecto.PerfectoIDEConnectorUtil.getExecutionIdCapability;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteExecuteMethod;

import com.google.common.collect.ImmutableMap;
import com.qmetry.qaf.automation.ui.webdriver.CommandTracker;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;
import com.qmetry.qaf.automation.util.StringUtil;

/**
 * @author chirag.jayswal (chirag.jayswal@infostretch.com)
 */
public class PerfectoDriverListener extends QAFWebDriverCommandAdapter {
	@Override
	public void beforeCommand(QAFExtendedWebDriver driver,
			CommandTracker commandTracker) {
		if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.QUIT)) {
			try {
				String appName = (String) driver.getCapabilities()
						.getCapability("applicationName");
				if (StringUtil.isNotBlank(appName) && StringUtil.isBlank((String) driver
						.getCapabilities().getCapability("eclipseExecutionId"))) {
					try {
						// open application command
						String command = "mobile:application:close";
						// open application
						driver.executeScript(command, ImmutableMap.of("name", appName));
					} catch (Error e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				driver.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void beforeInitialize(Capabilities desiredCapabilities) {
		if (getBundle().getString("remote.server", "").contains("perfecto")) {
			String eclipseExecutionId = getExecutionIdCapability();

			if (StringUtil.isNotBlank(eclipseExecutionId)) {
				((DesiredCapabilities) desiredCapabilities)
						.setCapability("eclipseExecutionId", eclipseExecutionId);
			}
		}
	}

	@Override
	public void onInitialize(QAFExtendedWebDriver driver) {
		String appName =
				(String) driver.getCapabilities().getCapability("applicationName");
		if (StringUtil.isNotBlank(appName)) {
			try {
				// open application command
				String command = "mobile:application:close";
				// open application
				driver.executeScript(command, ImmutableMap.of("name", appName));
			} catch (Error e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			openApplication(driver, appName);
		}
	}

	private void openApplication(QAFExtendedWebDriver driver, String appName) {
		PerfectoReportiumConnector.logTestStep("open '" + appName + "' application");
		// open application command
		String command = "mobile:application:open";
		// open application
		driver.executeScript(command, ImmutableMap.of("name", appName));
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(driver);
		executeMethod.execute(DriverCommand.SWITCH_TO_CONTEXT,
				ImmutableMap.of("name", "NATIVE"));

	}

}
