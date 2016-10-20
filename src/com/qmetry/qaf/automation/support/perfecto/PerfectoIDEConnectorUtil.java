/**
 * 
 */
package com.qmetry.qaf.automation.support.perfecto;

import com.perfectomobile.selenium.util.EclipseConnector;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.perfectomobile.intellij.connector.ConnectorConfiguration;
import com.perfectomobile.intellij.connector.impl.client.ClientSideLocalFileSystemConnector;
import com.perfectomobile.intellij.connector.impl.client.ProcessOutputLogAdapter;
/**
 * @author chirag.jayswal
 */
public class PerfectoIDEConnectorUtil {

	public static String getExecutionIdCapability() {

		try {
			String pluginType =
					ConfigurationManager.getBundle().getString("driver.pluginType", "");

			if ("eclipse".equalsIgnoreCase(pluginType)) {
				EclipseConnector connector;
				connector = new EclipseConnector();
				if (connector.getHost() != null) {
					return connector.getExecutionId();
				}
			}
			if ("intellij".equalsIgnoreCase(pluginType)) {
				ClientSideLocalFileSystemConnector intellijConnector =
						new ClientSideLocalFileSystemConnector(
								new ProcessOutputLogAdapter(System.err, System.out,
										System.out, System.out));
				ConnectorConfiguration connectorConfiguration =
						intellijConnector.getConnectorConfiguration();
				if (connectorConfiguration != null
						&& connectorConfiguration.getHost() != null) {
					return connectorConfiguration.getExecutionId();
				}
			}

		} catch (Exception e) {
			System.err.println("Could not connect to device opened in IDE");
		}
		return "";
	}

}
