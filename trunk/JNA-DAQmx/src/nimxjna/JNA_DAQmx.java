package nimxjna;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;

/**
 * Public class that creates a JNA interface to NationalInstruments DAQMX functions. 
 * The function calls themselves are all contained in the interface NiJNA which has 
 * to match the daqmx function calls 1:1. These can all be accessed through calls to 
 * the public instance of the library ni.nifunctionmame. The main class also contains a number of 
 * utility functions that wrap the main NI functions is a more user friendly manner.  
 * @author Doug Gillespie
 *
 */
public class JNA_DAQmx {

	public NiJNA ni = null;
	private long loadTime;


	/**
	 * Constructor for the library interface. 
	 * @throws DAQmxException thrown if the library cannot be loaded 
	 * (e.g. if NI drivers are not installed or on an unsupported platform) 
	 */
	public JNA_DAQmx() throws DAQmxException {
		super();
		long loadStart = System.nanoTime();
		String lib = "nicaiu";
		try {
			ni = Native.load(lib, NiJNA.class);
		}
		catch (Error e) {
			throw new DAQmxException(String.format("National instruments library %s is not available: %s", lib, e.getMessage()));
		}
		loadTime = System.nanoTime() - loadStart;
	}

	/**
	 * Get the time taken for the library to load 
	 * @return the loadTime
	 */
	public long getLoadTime() {
		return loadTime;
	}
	
	/**
	 * Get a list of available devices. 
	 * @return list of devices.
	 */
	public String[] getDeviceList() {
		int NAMELEN = 80;
		byte[] devNames = new byte[NAMELEN];
//		String[] st = new String[1];
		int ans = ni.DAQmxGetSysDevNames(devNames, NAMELEN);
		String names = new String(devNames).trim();
		return names.split(",");
	}
	
	/**
	 * Get the number of analog input channels. 
	 * @param device device name
	 * @return number of input channels or -1 if an error
	 */
	public int getNumAIChannels(String device) {
		String[] chans = getAIChannelNames(device);
		if (chans == null) {
			return -1;
		}
		return chans.length;
	}
	
	/**
	 * Get a list of channel names. 
	 * @param device device name
	 * @return list of channel names. 
	 */
	public String[] getAIChannelNames(String device) {
		byte[] data = new byte[2048];
		int ans = ni.DAQmxGetDevAIPhysicalChans(device, data, data.length);
		if (ans != 0) {
			return null;
		}
		String[] chans = new String(data).split(",");
		return chans;
	}

	/**
	 * Get the number of analog input channels. 
	 * @param device device name
	 * @return number of input channels or -1 if an error
	 */
	public int getNumAOChannels(String device) {
		byte[] data = new byte[2048];
		int ans = ni.DAQmxGetDevAOPhysicalChans(device, data, data.length);
		if (ans != 0) {
			return -1;
		}
		String[] chans = new String(data).split(",");
		return chans.length;
	}
	
	/**
	 * Get a devices analog input ranges. 
	 * @param device device name
	 * @return array of pairs of input ranges. 
	 */
	public double[][] getDeviceAIRanges(String device) {
		int MAXRANGES = 128;
		double[] ranges = new double[MAXRANGES];
		int ans = ni.DAQmxGetDevAIVoltageRngs(device, ranges, MAXRANGES);
		if (ans != 0) {
			return null;
		}
		// work out the number of ranges, which are in pairs and one of each pair must be non zero
		int nr = 0;
		for (int i = 0, j = 1; i < MAXRANGES; i+=2, j+=2) {
			if (ranges[i] != 0 || ranges[j] != 0) {
				nr++;
			}
		}
		double[][] pairRanges = new double[nr][2];
		
		for (int i = 0, j = 1, k = 0; i < MAXRANGES; i+=2, j+=2) {
			if (ranges[i] != 0 || ranges[j] != 0) {
				pairRanges[k][0] = ranges[i];
				pairRanges[k][1] = ranges[j];
				k++;
			}
		}
		
		return pairRanges;
	}

	/**
	 * Get a devices analog output ranges. 
	 * @param device device name
	 * @return array of pairs of input ranges. 
	 */
	public double[][] getDeviceAORanges(String device) {
		int MAXRANGES = 128;
		double[] ranges = new double[MAXRANGES];
		int ans = ni.DAQmxGetDevAOVoltageRngs(device, ranges, MAXRANGES);
		if (ans != 0) {
			return null;
		}
		// work out the number of ranges, which are in pairs and one of each pair must be non zero
		int nr = 0;
		for (int i = 0, j = 1; i < MAXRANGES; i+=2, j+=2) {
			if (ranges[i] != 0 || ranges[j] != 0) {
				nr++;
			}
		}
		double[][] pairRanges = new double[nr][2];
		
		for (int i = 0, j = 1, k = 0; i < MAXRANGES; i+=2, j+=2) {
			if (ranges[i] != 0 || ranges[j] != 0) {
				pairRanges[k][0] = ranges[i];
				pairRanges[k][1] = ranges[j];
				k++;
			}
		}
		
		return pairRanges;
	}
	
	public String getErrorString(int errorCode) {
		int bufLen = 1024;
		byte[] errBuf = new byte[bufLen];
		int ans = ni.DAQmxGetErrorString(errorCode, errBuf, bufLen);
		if (ans != 0) {
			return null;
		}
		return new String(errBuf).trim();
	}

	public interface NiJNA extends Library {

		/*
		 * This works when I pass it a byte array, but not when I pass it a char array. 
		 */
		public int DAQmxGetSysDevNames(byte[] data, int bufferSize);
		
		public int DAQmxGetDevSerialNum(String device, int[] data);
		
		public int DAQmxGetDevIsSimulated(String device, int[] data);

		public int DAQmxGetDevAISimultaneousSamplingSupported(String device, int[] data);

		public int DAQmxGetDevAIMaxSingleChanRate(String device, int[] data);

		public int DAQmxGetDevAIMaxMultiChanRate(String device, int[] data);

		public int DAQmxGetDevAIPhysicalChans(String device, byte[] data, int bufferSize);

		public int DAQmxGetDevAOPhysicalChans(String device, byte[] data, int bufferSize);

		public int DAQmxGetDevAIVoltageRngs(String device, double[] data, int bufferSize);

		public int DAQmxGetDevAOVoltageRngs(String device, double[] data, int bufferSize);

		public int DAQmxCreateTask(String taskName, LongByReference taskHandleRef);

		public int DAQmxStartTask(int taskHandle);
		
		public int DAQmxStopTask(int taskHandle);

		public int DAQmxClearTask(int taskHandle);

		public int DAQmxReadAnalogF64(int taskHandle, int numSampsPerChan, double timeout, int fillMode, double readArray[], 
				int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		
		public int DAQmxReadRaw (int taskHandle, int numSampsPerChan, double timeout,  byte[] readArray, 
				int arraySizeInBytes, IntByReference sampsRead, IntByReference numBytesPerSamp, IntByReference reserved);

		public int DAQmxCreateAIVoltageChan(int taskHandle, String physicalChannel, String nameToAssignToChannel, 
				int terminalConfig, double minVal, double maxVal, int units, String customScaleName);

		public int DAQmxSetAIRnghigh(int taskHandle, String channel, double data);

		public int DAQmxSetAIRngLow(int taskHandle, String channel, double data);
		
		public int DAQmxReadAnalogScalarF64 (int taskHandle, double timeout, DoubleByReference value, IntByReference reserved);
		
		public int DAQmxGetErrorString(int errorCode, byte[] errorString, int bufferSize);
		
		public int DAQmxCfgSampClkTiming(int taskHandle, String source, double rate, int activeEdge, int sampleMode, long sampsPerChan);
		
		/*
		 * 
typedef int32 (CVICALLBACK *DAQmxEveryNSamplesEventCallbackPtr)(TaskHandle taskHandle, int32 everyNsamplesEventType, uInt32 nSamples, void *callbackData);
typedef int32 (CVICALLBACK *DAQmxDoneEventCallbackPtr)(TaskHandle taskHandle, int32 status, void *callbackData);
typedef int32 (CVICALLBACK *DAQmxSignalEventCallbackPtr)(TaskHandle taskHandle, int32 signalID, void *callbackData);
https://github.com/java-native-access/jna/blob/master/www/CallbacksAndClosures.md
		 */
		public interface DAQmxEveryNSamplesEventCallbackPtr extends Callback {
			public int callback(int taskHandle, int everyNsamplesEventType, int nSamples, IntByReference callbackData);
		}
		public interface DAQmxDoneEventCallbackPtr extends Callback {
			public int callback(int taskHandle, int status, IntByReference callbackData);
		}
		public interface DAQmxSignalEventCallbackPtr extends Callback {
			public int callback(int taskHandle, int signalID, IntByReference callbackData);
		}

		public int DAQmxRegisterEveryNSamplesEvent (int task, int everyNsamplesEventType, int nSamples, int options, DAQmxEveryNSamplesEventCallbackPtr callbackFunction, IntByReference callbackData);
		public int DAQmxRegisterDoneEvent          (int task, int options, DAQmxDoneEventCallbackPtr callbackFunction,  IntByReference callbackData);
		public int DAQmxRegisterSignalEvent        (int task, int signalID, int options, DAQmxSignalEventCallbackPtr callbackFunction,  IntByReference callbackData);

	}
}
