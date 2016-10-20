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
import static com.qmetry.qaf.automation.step.PerfectoMobileSteps.installApp;
import static com.qmetry.qaf.automation.support.perfecto.PerfectoUtils.getDeviceIds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;

/**
 * @author chirag.jayswal
 */
public class PefectoDeviceAppInstaller {
	private static final String REPOSITORY_KEY = "perfecto.repository.folder";

	@Test(dataProvider = "deviceListProvider")
	public void installAppOnDevice(String deviceId) {
		System.out.println(deviceId);
		getBundle().setProperty("driver.name", "appiumRemoteDriver");

		getBundle().setProperty("driver.capabilities.deviceName", deviceId);
		QAFExtendedWebDriver driver = new WebDriverTestBase().getDriver();

		installApp(REPOSITORY_KEY,
				getBundle().getString("app.instrumentaion", "noinstrument"));
		// installApp(driver);
		driver.quit();
	}

	@DataProvider(name = "deviceListProvider")
	private static Iterator<Object[]> getConnectedDevices(ITestContext context)
			throws ConfigurationException {
		List<Object[]> lst = new ArrayList<Object[]>();

		Map<String, String> params = new HashMap<String, String>();

		params.put("os", "driver.capabilities.platformName");
		params.put("inUse", "false");

		for (Object deviceId : getDeviceIds(params)) {
			lst.add(new Object[]{deviceId});
		}

		return lst.iterator();
	}

	// private void installApp(QAFExtendedWebDriver driver) {
	// Map<String, Object> params = new HashMap<String, Object>();
	// params.put("file", getBundle().getString(REPOSITORY_KEY));
	// params.put("instrument", getBundle().getString("app.instrumentaion",
	// "noinstrument"));
	//
	// String resultStr = (String)
	// driver.executeScript("mobile:application:install", params);
	// System.out.println(resultStr);
	// }

}
