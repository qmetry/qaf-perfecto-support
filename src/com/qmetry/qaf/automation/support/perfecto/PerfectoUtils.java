/**
 * 
 */
package com.qmetry.qaf.automation.support.perfecto;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.ConfigurationException;
import org.hamcrest.Matchers;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.qmetry.qaf.automation.core.AutomationError;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.util.PropertyUtil;
import com.qmetry.qaf.automation.util.StringUtil;
import com.qmetry.qaf.automation.util.Validator;
import com.qmetry.qaf.automation.ws.rest.RestTestBase;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @author chirag.jayswal
 */
public class PerfectoUtils {

	// private static final Logger logger =
	// LoggerFactory.getLogger(PerfectoUtils.class);

	private static final String HTTPS = "https://";
	private static final String MEDIA_REPOSITORY = "/services/repositories/media/";
	private static final String LIST_HANDSETS = "/services/handsets?operation=list";

	private static final String UPLOAD_OPERATION = "operation=upload&overwrite=true";
	private static final String UTF_8 = "UTF-8";

	public static String getPerfectoHost(String url) {
		String hostSufix = "perfectomobile.com";
		if (StringUtil.isBlank(url) || url.indexOf(hostSufix) <= 0) {
			throw new RuntimeException("Invalide perfecto remote url: " + url);
		}

		return url.substring(0, url.indexOf(hostSufix)) + hostSufix;
	}

	/**
	 * refer https://community.perfectomobile.com/series/20095/posts/943233 for
	 * parameters
	 * 
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getDeviceIds(Map<String, String> params) {
		try {
			return readDevicesProperties(params).getList("handset.deviceId");
		} catch (Exception e) {
			throw new AutomationError("Unable to get device id", e);
		}
	}
	
	private static PropertyUtil readDevicesProperties(Map<String, String> params) throws ConfigurationException, UnsupportedEncodingException{
			RestTestBase testBase = new RestTestBase();
			String cloudeBaseUrl =
					getPerfectoHost(ApplicationProperties.REMOTE_SERVER.getStringVal());
			WebResource resource = testBase.getClient().resource(
					cloudeBaseUrl + LIST_HANDSETS + "&" + getQueryString(params));
			ClientResponse resp = resource.get(ClientResponse.class);

			PropertyUtil devices = new PropertyUtil();
			InputStream stream = new ByteArrayInputStream(
					resp.getEntity(String.class).getBytes(StandardCharsets.UTF_8));
			devices.load(stream);

			return devices;
			
	}
	
	/**
	 * refer https://community.perfectomobile.com/series/20095/posts/943233 for
	 * parameters
	 * TODO: need to complete implementation.....
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<Object, Object> getDeviceProperties(Map<String, String> params) {
		try {
			return ConfigurationConverter.getMap(readDevicesProperties(params).subset("handset"));
		} catch (Exception e) {
			throw new AutomationError("Unable to get device id", e);
		}
	}

	/**
	 * refer https://community.perfectomobile.com/series/20095/posts/943233 for
	 * parameters
	 * 
	 * @param params
	 * @return
	 */
	public static List<String> getDeviceIds(DesiredCapabilities capabilities) {
		try {

			Map<String, String> paramMap = new HashMap<String, String>();
			Map<String, ?> capMap = capabilities.asMap();
			for (String key : capMap.keySet()) {
				String pkey = key;
				if (key.equalsIgnoreCase(CapabilityType.PLATFORM)) {
					pkey = "os";
				}
				if (key.equalsIgnoreCase(CapabilityType.VERSION)) {
					pkey = "osVersion";
				}
				paramMap.put(pkey, capMap.get(key).toString());
			}
			if (!paramMap.containsKey("inUse")) {
				paramMap.put("inUse", "false");
			}
			return getDeviceIds(paramMap);
		} catch (Exception e) {
			throw new AutomationError("Unable to get device id", e);
		}
	}

	/**
	 * Download the report. type - pdf, html, csv, xml Example:
	 * downloadReport(driver, "pdf", "C:\\test\\report");
	 */

	public static void downloadReport(String type, String fileName) throws IOException {
		try {
			String command = "mobile:report:download";
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("type", type);
			String report = (String) new WebDriverTestBase().getDriver()
					.executeScript(command, params);
			File reportFile = new File(fileName + "." + type);
			BufferedOutputStream output =
					new BufferedOutputStream(new FileOutputStream(reportFile));
			byte[] reportBytes = OutputType.BYTES.convertFromBase64Png(report);
			output.write(reportBytes);
			output.close();
		} catch (Exception ex) {
			System.out.println("Got exception " + ex);
		}
	}

	/**
	 * Download all the report attachments with a certain type. type - video,
	 * image, vital, network Examples: downloadAttachment(driver, "video",
	 * "C:\\test\\report\\video", "flv"); downloadAttachment(driver, "image",
	 * "C:\\test\\report\\images", "jpg");
	 */
	public static void downloadAttachment(String type, String fileName, String suffix)
			throws IOException {
		try {
			String command = "mobile:report:attachment";
			boolean done = false;
			int index = 0;

			while (!done) {
				Map<String, Object> params = new HashMap<String, Object>();

				params.put("type", type);
				params.put("index", Integer.toString(index));

				String attachment = (String) new WebDriverTestBase().getDriver()
						.executeScript(command, params);

				if (attachment == null) {
					done = true;
				} else {
					File file = new File(fileName + index + "." + suffix);
					BufferedOutputStream output =
							new BufferedOutputStream(new FileOutputStream(file));
					byte[] bytes = OutputType.BYTES.convertFromBase64Png(attachment);
					output.write(bytes);
					output.close();
					index++;
				}
			}
		} catch (Exception ex) {
			System.out.println("Got exception " + ex);
		}
	}

	/**
	 * Uploads a file to the media repository. Example:
	 * uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com",
	 * "123456", "C:\\test\\ApiDemos.apk", "PRIVATE:apps/ApiDemos.apk");
	 */
	public static void uploadMedia(String host, String user, String password, String path,
			String repositoryKey) throws IOException {
		File file = new File(path);
		byte[] content = readFile(file);
		uploadMedia(host, user, password, content, repositoryKey);
	}

	/**
	 * Uploads a file to the media repository. Example: URL url = new URL(
	 * "http://file.appsapk.com/wp-content/uploads/downloads/Sudoku%20Free.apk")
	 * ; uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com",
	 * "123456", url, "PRIVATE:apps/ApiDemos.apk");
	 */
	public static void uploadMedia(String host, String user, String password,
			URL mediaURL, String repositoryKey) throws IOException {
		byte[] content = readURL(mediaURL);
		uploadMedia(host, user, password, content, repositoryKey);
	}

	/**
	 * Uploads content to the media repository. Example:
	 * uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com",
	 * "123456", content, "PRIVATE:apps/ApiDemos.apk");
	 */
	public static void uploadMedia(String host, String user, String password,
			byte[] content, String repositoryKey)
			throws UnsupportedEncodingException, MalformedURLException, IOException {
		if (content != null) {
			String encodedUser = URLEncoder.encode(user, "UTF-8");
			String encodedPassword = URLEncoder.encode(password, "UTF-8");
			String urlStr = HTTPS + host + MEDIA_REPOSITORY + repositoryKey + "?"
					+ UPLOAD_OPERATION + "&user=" + encodedUser + "&password="
					+ encodedPassword;
			URL url = new URL(urlStr);

			sendRequest(content, url);
		}
	}

	private static String getQueryString(Map<String, String> params)
			throws UnsupportedEncodingException {

		StringBuilder sb = new StringBuilder();
		if (!params.containsKey("user") || !params.containsKey("password")) {
			params.put("user", "driver.capabilities.user");
			params.put("password", "driver.capabilities.password");
		}
		for (Entry<String, String> e : params.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.displayName()))
					.append('=')
					.append(URLEncoder.encode(
							getBundle().getString(e.getValue(), e.getValue()),
							StandardCharsets.UTF_8.displayName()));
		}
		return sb.toString();
	}

	private static void sendRequest(byte[] content, URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/octet-stream");
		connection.connect();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		outStream.write(content);
		outStream.writeTo(connection.getOutputStream());
		outStream.close();
		int code = connection.getResponseCode();
		if (code > HttpURLConnection.HTTP_OK) {
			handleError(connection);
		}
	}

	private static void handleError(HttpURLConnection connection) throws IOException {
		String msg = "Failed to upload media.";
		InputStream errorStream = connection.getErrorStream();
		if (errorStream != null) {
			InputStreamReader inputStreamReader =
					new InputStreamReader(errorStream, UTF_8);
			BufferedReader bufferReader = new BufferedReader(inputStreamReader);
			try {
				StringBuilder builder = new StringBuilder();
				String outputString;
				while ((outputString = bufferReader.readLine()) != null) {
					if (builder.length() != 0) {
						builder.append("\n");
					}
					builder.append(outputString);
				}
				String response = builder.toString();
				msg += "Response: " + response;
			} finally {
				bufferReader.close();
			}
		}
		throw new RuntimeException(msg);
	}

	private static byte[] readFile(File path) throws FileNotFoundException, IOException {
		int length = (int) path.length();
		byte[] content = new byte[length];
		InputStream inStream = new FileInputStream(path);
		try {
			inStream.read(content);
		} finally {
			inStream.close();
		}
		return content;
	}

	private static byte[] readURL(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		int code = connection.getResponseCode();
		if (code > HttpURLConnection.HTTP_OK) {
			handleError(connection);
		}
		InputStream stream = connection.getInputStream();

		if (stream == null) {
			throw new RuntimeException(
					"Failed to get content from url " + url + " - no response stream");
		}
		byte[] content = read(stream);
		return content;
	}

	private static byte[] read(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int nBytes = 0;
			while ((nBytes = input.read(buffer)) > 0) {
				output.write(buffer, 0, nBytes);
			}
			byte[] result = output.toByteArray();
			return result;
		} finally {
			try {
				input.close();
			} catch (IOException e) {

			}
		}
	}

	public static boolean verifyVisualText(RemoteWebDriver driver, String text) {
		return Validator.verifyThat(isText(driver, text, null), Matchers.equalTo("true"));
	}

	public static void assertVisualText(RemoteWebDriver driver, String text) {
		Validator.assertThat("Text: \"" + text + "\" should be present",
				isText(driver, text, null), Matchers.equalTo("true"));
	}

	public static void installApp(String filePath, RemoteWebDriver d,
			boolean shouldInstrument) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("file", filePath);
		if (shouldInstrument) {
			params.put("instrument", "instrument");
		}
		d.executeScript("mobile:application:install", params);
	}

	private static Map<String, String> getAppParams(String app, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, app);
		return params;
	}

	// by = "name" or "identifier"
	public static void startApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:open", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void closeApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:close", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void cleanApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:clean", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void uninstallApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:uninstall", getAppParams(app, by));
	}

	public static void uninstallAllApps(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		driver.executeScript("mobile:application:reset", params);
	}

	public static String getAppInfo(RemoteWebDriver driver, String property) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("property", property);
		return (String) driver.executeScript("mobile:application:info", params);
	}

	public static boolean verifyAppInfo(RemoteWebDriver driver, String propertyName,
			String propertyValue) {
		return Validator.verifyThat(getAppInfo(driver, propertyName),
				Matchers.equalTo(propertyValue));
	}

	public static void assertAppInfo(RemoteWebDriver driver, String propertyName,
			String propertyValue) {
		String appOrientation = getAppInfo(driver, propertyName);
		Validator.assertThat(appOrientation, Matchers.equalTo(propertyValue));
	}

	public static void switchToContext(RemoteWebDriver driver, String context) {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(driver);
		Map<String, String> params = new HashMap<String, String>();
		params.put("name", context);
		executeMethod.execute(DriverCommand.SWITCH_TO_CONTEXT, params);
	}

	public static void waitForPresentTextVisual(RemoteWebDriver driver, String text,
			int seconds) {
		isText(driver, text, seconds);
	}

	public static void waitForPresentImageVisual(RemoteWebDriver driver, String image,
			int seconds) {
		isImg(driver, image, seconds);
	}

	private static String isImg(RemoteWebDriver driver, String img, Integer timeout) {
		String context = getCurrentContext(driver);
		switchToContext(driver, "VISUAL");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("content", img);
		params.put("measurement", "accurate");
		params.put("source", "primary");
		params.put("threshold", "90");
		params.put("timeout", timeout);
		params.put("match", "bounded");
		params.put("imageBounds.needleBound", 25);
		Object result = driver.executeScript("mobile:checkpoint:image", params);
		switchToContext(driver, context);
		return result.toString();
	}

	public static void assertVisualImg(RemoteWebDriver driver, String img) {
		Validator.assertThat("Image " + img + " should be visible",
				isImg(driver, img, 180), Matchers.equalTo("true"));
	}

	public static boolean verifyVisualImg(RemoteWebDriver driver, String img) {
		return Validator.verifyThat(isImg(driver, img, 180), Matchers.equalTo("true"));
	}

	private static String isText(RemoteWebDriver driver, String text, Integer timeout) {
		String context = getCurrentContext(driver);
		switchToContext(driver, "VISUAL");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("content", text);
		if (timeout != null) {
			params.put("timeout", timeout);
		}
		Object result = driver.executeScript("mobile:checkpoint:text", params);
		switchToContext(driver, context);
		return result.toString();
	}

	/**
	 * @param driver
	 * @return the current context - "NATIVE_APP", "WEBVIEW", "VISUAL"
	 */
	public static String getCurrentContext(RemoteWebDriver driver) {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(driver);
		return (String) executeMethod.execute(DriverCommand.GET_CURRENT_CONTEXT_HANDLE,
				null);
	}

	// device utils

	/**
	 * Clicks on a single or sequence of physical device keys.
	 * Mouse-over the device keys to identify them, then input into the Keys
	 * parameter according to the required syntax.
	 * <p>
	 * Common keys include:
	 * LEFT, RIGHT, UP, DOWN, OK, BACK, MENU, VOL_UP, VOL_DOWN, CAMERA, CLEAR.
	 * <p>
	 * The listed keys are not necessarily supported by all devices. The
	 * available keys depend on the device.
	 *
	 * @param driver
	 *            the RemoteWebDriver
	 * @param keySequence
	 *            the single or sequence of keys to click
	 */
	public static void pressKey(RemoteWebDriver driver, String keySequence) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("keySequence", keySequence);
		driver.executeScript("mobile:presskey", params);
	}

	/**
	 * Performs the swipe gesture according to the start and end coordinates.
	 * <p>
	 * Example swipe left:<br/>
	 * start: 60%,50% end: 10%,50%
	 *
	 * @param driver
	 *            the RemoteWebDriver
	 * @param start
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 * @param end
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void swipe(RemoteWebDriver driver, String start, String end) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("start", start);
		params.put("end", end);

		driver.executeScript("mobile:touch:swipe", params);

	}

	/**
	 * Performs the touch gesture according to the point coordinates.
	 * 
	 * @param driver
	 *            the RemoteWebDriver
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void touch(RemoteWebDriver driver, String point) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("location", point); // 50%,50%

		driver.executeScript("mobile:touch:tap", params);
	}

	/**
	 * Hides the virtual keyboard display.
	 * 
	 * @param driver
	 *            the RemoteWebDriver
	 */
	public static void hideKeyboard(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mode", "off");

		driver.executeScript("mobile:keyboard:display", params);

	}

	/**
	 * Rotates the device to landscape, portrait, or its next state.
	 * 
	 * @param driver
	 *            the RemoteWebDriver
	 * @param restValue
	 *            the "next" operation, or the "landscape" or "portrait" state.
	 * @param by
	 *            the "state" or "operation"
	 */
	// TODO: need additional description.
	public static void rotateDevice(RemoteWebDriver driver, String restValue, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, restValue);
		driver.executeScript("mobile:handset:rotate", params);
	}

	// by = "address" or "coordinates"
	public static void setLocation(RemoteWebDriver driver, String location, String by) {

		Map<String, String> params = new HashMap<String, String>();
		params.put(by, location);

		driver.executeScript("mobile:location:set", params);
	}

	public static void assertLocation(RemoteWebDriver driver, String location) {
		String deviceLocation = getDeviceLocation(driver);
		Validator.assertThat("The device location", deviceLocation,
				Matchers.equalTo(location));

	}

	public static boolean verifyLocation(RemoteWebDriver driver, String location) {
		String deviceLocation = getDeviceLocation(driver);
		return Validator.verifyThat(deviceLocation, Matchers.equalTo(location));
	}

	public static String getDeviceLocation(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		return (String) driver.executeScript("mobile:location:get", params);
	}

	public static void resetLocation(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		driver.executeScript("mobile:location:reset", params);
	}

	public static void goToHomeScreen(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("target", "All");

		driver.executeScript("mobile:handset:ready", params);
	}

	public static void lockDevice(RemoteWebDriver driver, int sec) {
		Map<String, Integer> params = new HashMap<String, Integer>();
		params.put("timeout", sec);

		driver.executeScript("mobile:screen:lock", params);
	}

	public static void setTimezone(RemoteWebDriver driver, String timezone) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("timezone", timezone);

		driver.executeScript("mobile:timezone:set", params);
	}

	public static String getTimezone(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();

		return (String) driver.executeScript("mobile:timezone:get", params);
	}

	public static void assertTimezone(RemoteWebDriver driver, String timezone) {
		String deviceTimezone = getTimezone(driver);
		Validator.assertThat("The device timezone", deviceTimezone,
				Matchers.equalTo(timezone));
	}

	public static boolean verifyTimezone(RemoteWebDriver driver, String timezone) {
		return Validator.verifyThat(getTimezone(driver), Matchers.equalTo(timezone));
	}

	public static void resetTimezone(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		driver.executeScript("mobile:timezone:reset", params);
	}

	public static void takeScreenshot(RemoteWebDriver driver, String repositoryPath,
			boolean shouldSave) {
		Map<String, String> params = new HashMap<String, String>();
		if (shouldSave) {
			params.put("key", repositoryPath);
		}
		driver.executeScript("mobile:screen:image", params);
	}

	/**
	 * Checks if is device.
	 *
	 * @param driver
	 *            capabilities
	 * @return true, if is device
	 */
	public static boolean isDevice(Capabilities caps) {
		// first check if driver is a mobile device:
		if (isDesktopBrowser(caps))
			return false;
		return caps.getCapability("deviceName") != null;
	}

	public static boolean isDesktopBrowser(Capabilities caps) {
		// first check if deviceName set to browser name which triggers desktop:
		return Arrays
				.asList("firefox", "chrome", "iexplorer", "internet explorer", "safari")
				.contains((caps.getCapability("browserVersion") + "").toLowerCase());
	}

	/**
	 * Checks if is device.
	 *
	 *TODO: complete me
	 * @param driver
	 *            the driver
	 * @return true, if is device
	 */
	public static boolean isDevice(RemoteWebDriver driver) {
		return isDevice(driver.getCapabilities());
	}
	
	 public static Map<String, Object> getDeviceProperties(Capabilities capabilities) {
	        Map<String, Object> deviceProperties = new HashMap<String, Object>();

	        if (!isDevice(capabilities))
	            return deviceProperties;

	        try {
			//	return ConfigurationConverter.getMap(readDevicesProperties(params).subset("handset"));
			} catch (Exception e) {
				throw new AutomationError("Unable to get device id", e);
			}
	        return deviceProperties;
	}
}
