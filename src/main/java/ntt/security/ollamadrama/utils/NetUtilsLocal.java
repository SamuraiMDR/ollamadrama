package ntt.security.ollamadrama.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.MCPEndpoint;
import ntt.security.ollamadrama.objects.OllamaEndpoint;

public class NetUtilsLocal {

	private static final Logger LOGGER = LoggerFactory.getLogger(NetUtilsLocal.class);

	public static boolean isValidIPV4(final String ipv4String) {
		if (null == ipv4String) {
			return false;
		} else {
			// Optimization
			if ("::1".equals(ipv4String)) {
				return false;
			}
			if ("".equals(ipv4String)) {
				return false;
			}

			// Length optimization, xxx.xxx.xxx.xxx = 15, x.x.x.x = 7
			if ((ipv4String.length() > 15) || (ipv4String.length() < 7)) {
				return false;
			}

			// Make sure first and last char is a digit
			if (Character.isDigit(ipv4String.charAt(0)) && Character.isDigit(ipv4String.charAt(ipv4String.length()-1))) {
				// Make sure we have 4 dots
				final String[] octets = ipv4String.split("\\.");
				if (octets.length == 4) {
					try {
						int octet1 = Integer.parseInt(octets[0]);
						int octet2 = Integer.parseInt(octets[1]);
						int octet3 = Integer.parseInt(octets[2]);
						int octet4 = Integer.parseInt(octets[3]);
						if (true &&
								(octets[0].length()<=3) &&
								(octets[1].length()<=3) &&
								(octets[2].length()<=3) &&
								(octets[3].length()<=3) &&
								(octet1 >= 0) &&
								(octet1 <= 255) &&
								(octet2 >= 0) &&
								(octet2 <= 255) &&
								(octet3 >= 0) &&
								(octet3 <= 255) &&
								(octet4 >= 0) &&
								(octet4 <= 255) &&
								true) {
							return true;
						}
					} catch (Exception e) {
						return false;
					}
				}
			}
		}
		return false;
	}

	public static ArrayList<String> determineLocalIPv4s() {
		ArrayList<String> localips = new ArrayList<String>();
		localips.add("127.0.0.1");
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp())
					continue;
				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while(addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					String ip = addr.getHostAddress();
					if (isValidIPV4(ip)) localips.add(ip);
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		return localips;
	}

	public static String grabCnetworkSlice(final String ip) {
		final String octets[] = ip.split("\\.");
		if (octets.length == 4) {
			return octets[0] + "." + octets[1] + "." + octets[2];
		} else {
			return ip;
		}
	}


	public static TreeMap<String, OllamaEndpoint> performTCPPortSweepForOllama(int port, ArrayList<String> cnets, int startIP, int stopIP, int timeout, int threadPoolCount, String _username, String _password) {
		TreeMap<String, OllamaEndpoint> activeEndpoints = new TreeMap<String, OllamaEndpoint>();
		if (stopIP<startIP) {
			LOGGER.error("stopIP (" + stopIP + ") cannot be smaller than startIP (" + startIP + ")");
			SystemUtils.halt();
		}
		if (port>65535) {
			LOGGER.error("invalid port " + port + " specified as input");
			SystemUtils.halt();
		}
		if (port<0) {
			LOGGER.error("invalid port " + port + " specified as input");
			SystemUtils.halt();
		}
		try {
			final ExecutorService es = Executors.newFixedThreadPool(threadPoolCount);
			final List<Future<String>> futures = new ArrayList<>();
			for (int lastOctet = startIP; lastOctet <= stopIP; lastOctet++) {
				for (String cnet: cnets) {
					if ("127.0.0".equals(cnet)) {
						if (lastOctet>1) continue;
					}
					String ip = cnet + "." + lastOctet;
					if (!NetUtilsLocal.isValidIPV4(ip)) {
						LOGGER.error("Invalid ip generated from input parameters: " + ip);
						SystemUtils.halt();
					}
					futures.add(portIsOpenForHost(es, ip, port, timeout));
				}
			}
			es.shutdown();
			for (final Future<String> f : futures) {
				if (!"".equals(f.get())) {
					String endpoint = f.get();
					activeEndpoints.put("http://" + endpoint + ":" + port, new OllamaEndpoint("http://" + endpoint + ":" + port , _username, _password));
				}
			}
		} catch (Exception ex) {
			LOGGER.warn("performTCPPortSweep ex: " + ex.getMessage());
		}
		return activeEndpoints;
	}

	public static TreeMap<String, MCPEndpoint> performTCPPortSweepForMCP(ArrayList<Integer> ports, ArrayList<String> cnets, int startIP, int stopIP, int timeout, int threadPoolCount) {
		TreeMap<String, MCPEndpoint> activeEndpoints = new TreeMap<String, MCPEndpoint>();
		if (stopIP<startIP) {
			LOGGER.error("stopIP (" + stopIP + ") cannot be smaller than startIP (" + startIP + ")");
			SystemUtils.halt();
		}

		for (int port: ports) {
			if (port>65535) {
				LOGGER.error("invalid port " + port + " specified as input");
				SystemUtils.halt();
			}
			if (port<0) {
				LOGGER.error("invalid port " + port + " specified as input");
				SystemUtils.halt();
			}
			try {
				final ExecutorService es = Executors.newFixedThreadPool(threadPoolCount);
				final List<Future<String>> futures = new ArrayList<>();
				for (int lastOctet = startIP; lastOctet <= stopIP; lastOctet++) {
					for (String cnet: cnets) {
						if ("127.0.0".equals(cnet)) {
							if (lastOctet>1) continue;
						}
						String ip = cnet + "." + lastOctet;
						if (!NetUtilsLocal.isValidIPV4(ip)) {
							LOGGER.error("Invalid ip generated from input parameters: " + ip);
							SystemUtils.halt();
						}
						futures.add(portIsOpenForHost(es, ip, port, timeout));
					}
				}
				es.shutdown();
				for (final Future<String> f : futures) {
					if (!"".equals(f.get())) {
						String endpoint = f.get();
						// schema and path not yet verified/checked
						activeEndpoints.put(endpoint + "::" + port, new MCPEndpoint("", endpoint, port, ""));
					}
				}
			} catch (Exception ex) {
				LOGGER.warn("performTCPPortSweep ex: " + ex.getMessage());
			}
		}
		return activeEndpoints;
	}

	public static ArrayList<Integer> performTCPHostScan(String ip, int startPort, int stopPort, int timeout, int threadPoolCount) {
		ArrayList<Integer> openPorts = new ArrayList<Integer>();
		if (stopPort<startPort) {
			LOGGER.error("stopPort (" + stopPort + ") cannot be smaller than startPort (" + startPort + ")");
			SystemUtils.halt();
		}
		if (startPort>65535) {
			LOGGER.error("invalid startPort " + startPort + " specified as input");
			SystemUtils.halt();
		}
		if (startPort<0) {
			LOGGER.error("invalid startPort " + startPort + " specified as input");
			SystemUtils.halt();
		}
		if (stopPort>65535) {
			LOGGER.error("invalid stopPort " + stopPort + " specified as input");
			SystemUtils.halt();
		}
		if (stopPort<0) {
			LOGGER.error("invalid stopPort " + stopPort + " specified as input");
			SystemUtils.halt();
		}
		try {
			final ExecutorService es = Executors.newFixedThreadPool(threadPoolCount);
			final List<Future<Integer>> futures = new ArrayList<>();
			for (int port = startPort; port <= stopPort; port++) {
				futures.add(portIsOpen(es, ip, port, timeout));
			}
			es.shutdown();
			for (final Future<Integer> f : futures) {
				if (f.get() != -1) {
					openPorts.add(f.get());
				}
			}
		} catch (Exception ex) {
		}
		return openPorts;
	}

	public static Future<Integer> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
		return es.submit(new Callable<Integer>() {
			@Override public Integer call() {
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(ip, port), timeout);
					socket.close();
					return port;
				} catch (Exception ex) {
					return -1;
				}
			}
		});
	}

	public static Future<String> portIsOpenForHost(final ExecutorService es, final String ip, final int port, final int timeout) {
		return es.submit(new Callable<String>() {
			@Override public String call() {
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(ip, port), timeout);
					socket.close();
					return ip;
				} catch (Exception ex) {
					return "";
				}
			}
		});
	}
}
