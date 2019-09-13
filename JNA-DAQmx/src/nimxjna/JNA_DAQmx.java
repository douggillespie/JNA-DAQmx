package nimxjna;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.ByteByReference;
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
 * <p>
 * It was also found that if all functions were included then Java could not load the library since the 
 * code size became too large. Probably, the best way to use this is to include the Java code in your own 
 * project and comment out all functions that you do not actually need in order to minimise load times. While not
 * as elegant as including a pre-built jar file it will probably work better  
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

	/**
	 * Interface defining all the DAQmx functions. These have been copy pasted from the DAQmx header file
	 * and modified to use Java parameters. Very few functions have been tested. 
	 * @author Doug Gillespie
	 *
	 */
	public interface NiJNA extends Library {

		//		/*
		//		 * This works when I pass it a byte array, but not when I pass it a byte array. 
		//		 */
		//		public int DAQmxGetSysDevNames(byte[] data, int bufferSize);
		//		
		//		public int DAQmxGetDevSerialNum(String device, int[] data);
		//		
		//		public int DAQmxGetDevIsSimulated(String device, int[] data);
		//
		//		public int DAQmxGetDevAISimultaneousSamplingSupported(String device, int[] data);
		//
		//		public int DAQmxGetDevAIMaxSingleChanRate(String device, int[] data);
		//
		//		public int DAQmxGetDevAIMaxMultiChanRate(String device, int[] data);
		//
		//		public int DAQmxGetDevAIPhysicalChans(String device, byte[] data, int bufferSize);
		//
		//		public int DAQmxGetDevAOPhysicalChans(String device, byte[] data, int bufferSize);
		//
		//		public int DAQmxGetDevAIVoltageRngs(String device, double[] data, int bufferSize);
		//
		//		public int DAQmxGetDevAOVoltageRngs(String device, double[] data, int bufferSize);
		//
		//		public int DAQmxCreateTask(String taskName, LongByReference taskHandleRef);
		//
		//		public int DAQmxStartTask(int taskHandle);
		//		
		//		public int DAQmxStopTask(int taskHandle);
		//
		//		public int DAQmxClearTask(int taskHandle);
		//
		//		public int DAQmxReadAnalogF64(int taskHandle, int numSampsPerChan, double timeout, int fillMode, double readArray[], 
		//				int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		//		
		//		public int DAQmxReadRaw (int taskHandle, int numSampsPerChan, double timeout,  byte[] readArray, 
		//				int arraySizeInBytes, IntByReference sampsRead, IntByReference numBytesPerSamp, IntByReference reserved);
		//
		//		public int DAQmxCreateAIVoltageChan(int taskHandle, String physicalChannel, String nameToAssignToChannel, 
		//				int terminalConfig, double minVal, double maxVal, int units, String customScaleName);
		//
		//		public int DAQmxSetAIRnghigh(int taskHandle, String channel, double data);
		//
		//		public int DAQmxSetAIRngLow(int taskHandle, String channel, double data);
		//		
		//		public int DAQmxReadAnalogScalarF64 (int taskHandle, double timeout, DoubleByReference value, IntByReference reserved);
		//		
		//		public int DAQmxGetErrorString(int errorCode, byte[] errorString, int bufferSize);
		//		
		//		public int DAQmxCfgSampClkTiming(int taskHandle, String source, double rate, int activeEdge, int sampleMode, long sampsPerChan);
		//		
		/*
		 * 
typedef int (CVICALLBACK *DAQmxEveryNSamplesEventCallbackPtr)(int taskHandle, int everyNsamplesEventType, int nSamples, ByReference callbackData);
typedef int (CVICALLBACK *DAQmxDoneEventCallbackPtr)(int taskHandle, int status, ByReference callbackData);
typedef int (CVICALLBACK *DAQmxSignalEventCallbackPtr)(int taskHandle, int signalID, ByReference callbackData);
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

		//		public int DAQmxRegisterEveryNSamplesEvent (int task, int everyNsamplesEventType, int nSamples, int options, DAQmxEveryNSamplesEventCallbackPtr callbackFunction, IntByReference callbackData);
		//		public int DAQmxRegisterDoneEvent          (int task, int options, DAQmxDoneEventCallbackPtr callbackFunction,  IntByReference callbackData);
		//		public int DAQmxRegisterSignalEvent        (int task, int signalID, int options, DAQmxSignalEventCallbackPtr callbackFunction,  IntByReference callbackData);


		/******************************************************/
		/***         Task Configuration/Control             ***/
		/******************************************************/


		public int     DAQmxLoadTask            (String taskName, LongByReference taskHandle);
		public int     DAQmxCreateTask          (String taskName, LongByReference taskHandle);
		// Channel Names must be valid channels already available in MAX. They are not created.
		public int     DAQmxAddGlobalChansToTask(int taskHandle, String channelNames);

		public int     DAQmxStartTask           (int taskHandle);
		public int     DAQmxStopTask            (int taskHandle);

		public int     DAQmxClearTask           (int taskHandle);

		public int     DAQmxWaitUntilTaskDone   (int taskHandle, double timeToWait);
		public int     DAQmxIsTaskDone          (int taskHandle, IntByReference isTaskDone);

		public int     DAQmxTaskControl         (int taskHandle, int action);

		public int     DAQmxGetNthTaskChannel   (int taskHandle, int index, byte buffer[], int bufferSize);

		public int     DAQmxGetNthTaskDevice    (int taskHandle, int index, byte buffer[], int bufferSize);

		//public int   DAQmxGetTaskAttribute    (int taskHandle, int attribute, ByReference value, ByReference... args);


		public int     DAQmxRegisterEveryNSamplesEvent (int taskHandle, int everyNsamplesEventType, int nSamples, int options, DAQmxEveryNSamplesEventCallbackPtr callbackFunction, Pointer callbackData);
		public int     DAQmxRegisterDoneEvent          (int taskHandle, int options, DAQmxDoneEventCallbackPtr callbackFunction, Pointer callbackData);
		public int     DAQmxRegisterSignalEvent        (int taskHandle, int signalID, int options, DAQmxSignalEventCallbackPtr callbackFunction, Pointer callbackData);

		/******************************************************/
		/***        Channel Configuration/Creation          ***/
		/******************************************************/


		public int     DAQmxCreateAIVoltageChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, String customScaleName);
		public int     DAQmxCreateAICurrentChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, int shuntResistorLoc, double extShuntResistorVal, String customScaleName);
		public int     DAQmxCreateAIVoltageRMSChan       (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, String customScaleName);
		public int     DAQmxCreateAICurrentRMSChan       (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, int shuntResistorLoc, double extShuntResistorVal, String customScaleName);
		public int     DAQmxCreateAIThrmcplChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int thermocoupleType, int cjcSource, double cjcVal, String cjcChannel);
		public int     DAQmxCreateAIRTDChan              (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int rtdType, int resistanceConfig, int currentExcitSource, double currentExcitVal, double r0);
		public int     DAQmxCreateAIThrmstrChanIex       (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int resistanceConfig, int currentExcitSource, double currentExcitVal, double a, double b, double c);
		public int     DAQmxCreateAIThrmstrChanVex       (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int resistanceConfig, int voltageExcitSource, double voltageExcitVal, double a, double b, double c, double r1);
		public int     DAQmxCreateAIFreqVoltageChan      (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, double thresholdLevel, double hysteresis, String customScaleName);
		public int     DAQmxCreateAIResistanceChan       (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int resistanceConfig, int currentExcitSource, double currentExcitVal, String customScaleName);
		public int     DAQmxCreateAIStrainGageChan       (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int strainConfig, int voltageExcitSource, double voltageExcitVal, double gageFactor, double initialBridgeVoltage, double nominalGageResistance, double poissonRatio, double leadWireResistance, String customScaleName);
		public int     DAQmxCreateAIVoltageChanWithExcit (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, int bridgeConfig, int voltageExcitSource, double voltageExcitVal, int useExcitForScaling, String customScaleName);
		public int     DAQmxCreateAITempBuiltInSensorChan(int taskHandle, String physicalChannel, String nameToAssignToChannel, int units);
		public int     DAQmxCreateAIAccelChan            (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, double sensitivity, int sensitivityUnits, int currentExcitSource, double currentExcitVal, String customScaleName);

		public int     DAQmxCreateAIMicrophoneChan       (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, int units, double micSensitivity, double maxSndPressLevel, int currentExcitSource, double currentExcitVal, String customScaleName);
		public int     DAQmxCreateAIPosLVDTChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, double sensitivity, int sensitivityUnits, int voltageExcitSource, double voltageExcitVal, double voltageExcitFreq, int ACExcitWireMode, String customScaleName);
		public int     DAQmxCreateAIPosRVDTChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, double sensitivity, int sensitivityUnits, int voltageExcitSource, double voltageExcitVal, double voltageExcitFreq, int ACExcitWireMode, String customScaleName);
		// Function DAQmxCreateAIDeviceTempChan is obsolete and has been replaced by DAQmxCreateAITempBuiltInSensorChan
		public int     DAQmxCreateAIDeviceTempChan       (int taskHandle, String physicalChannel, String nameToAssignToChannel, int units);

		//		public int     DAQmxCreateTEDSAIVoltageChan      (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, String customScaleName);
		//		public int     DAQmxCreateTEDSAICurrentChan      (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, int shuntResistorLoc, double extShuntResistorVal, String customScaleName);
		//		public int     DAQmxCreateTEDSAIThrmcplChan      (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int cjcSource, double cjcVal, String cjcChannel);
		//		public int     DAQmxCreateTEDSAIRTDChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int resistanceConfig, int currentExcitSource, double currentExcitVal);
		//		public int     DAQmxCreateTEDSAIThrmstrChanIex   (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int resistanceConfig, int currentExcitSource, double currentExcitVal);
		//		public int     DAQmxCreateTEDSAIThrmstrChanVex   (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int resistanceConfig, int voltageExcitSource, double voltageExcitVal, double r1);
		//		public int     DAQmxCreateTEDSAIResistanceChan   (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int resistanceConfig, int currentExcitSource, double currentExcitVal, String customScaleName);
		//		public int     DAQmxCreateTEDSAIStrainGageChan   (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int voltageExcitSource, double voltageExcitVal, double initialBridgeVoltage, double leadWireResistance, String customScaleName);
		//		public int     DAQmxCreateTEDSAIVoltageChanWithExcit (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, int voltageExcitSource, double voltageExcitVal, String customScaleName);
		//		public int     DAQmxCreateTEDSAIAccelChan        (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, double minVal, double maxVal, int units, int currentExcitSource, double currentExcitVal, String customScaleName);
		//
		//		public int     DAQmxCreateTEDSAIMicrophoneChan   (int taskHandle, String physicalChannel, String nameToAssignToChannel, int terminalConfig, int units, double maxSndPressLevel, int currentExcitSource, double currentExcitVal, String customScaleName);
		//		public int     DAQmxCreateTEDSAIPosLVDTChan      (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int voltageExcitSource, double voltageExcitVal, double voltageExcitFreq, int ACExcitWireMode, String customScaleName);
		//		public int     DAQmxCreateTEDSAIPosRVDTChan      (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, int voltageExcitSource, double voltageExcitVal, double voltageExcitFreq, int ACExcitWireMode, String customScaleName);
		//
		//		public int     DAQmxCreateAOVoltageChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, String customScaleName);
		//		public int     DAQmxCreateAOCurrentChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, double minVal, double maxVal, int units, String customScaleName);
		//		public int     DAQmxCreateAOFuncGenChan          (int taskHandle, String physicalChannel, String nameToAssignToChannel, int type, double freq, double amplitude, double offset);
		//
		//		public int     DAQmxCreateDIChan                 (int taskHandle, String lines, String nameToAssignToLines, int lineGrouping);
		//
		//		public int     DAQmxCreateDOChan                 (int taskHandle, String lines, String nameToAssignToLines, int lineGrouping);
		//
		//		public int     DAQmxCreateCIFreqChan             (int taskHandle, String counter, String nameToAssignToChannel, double minVal, double maxVal, int units, int edge, int measMethod, double measTime, int divisor, String customScaleName);
		//		public int     DAQmxCreateCIPeriodChan           (int taskHandle, String counter, String nameToAssignToChannel, double minVal, double maxVal, int units, int edge, int measMethod, double measTime, int divisor, String customScaleName);
		//		public int     DAQmxCreateCICountEdgesChan       (int taskHandle, String counter, String nameToAssignToChannel, int edge, int initialCount, int countDirection);
		//		public int     DAQmxCreateCIPulseWidthChan       (int taskHandle, String counter, String nameToAssignToChannel, double minVal, double maxVal, int units, int startingEdge, String customScaleName);
		//		public int     DAQmxCreateCISemiPeriodChan       (int taskHandle, String counter, String nameToAssignToChannel, double minVal, double maxVal, int units, String customScaleName);
		//		public int     DAQmxCreateCITwoEdgeSepChan       (int taskHandle, String counter, String nameToAssignToChannel, double minVal, double maxVal, int units, int firstEdge, int secondEdge, String customScaleName);
		//		public int     DAQmxCreateCILinEncoderChan       (int taskHandle, String counter, String nameToAssignToChannel, int decodingType, int ZidxEnable, double ZidxVal, int ZidxPhase, int units, double distPerPulse, double initialPos, String customScaleName);
		//		public int     DAQmxCreateCIAngEncoderChan       (int taskHandle, String counter, String nameToAssignToChannel, int decodingType, int ZidxEnable, double ZidxVal, int ZidxPhase, int units, int pulsesPerRev, double initialAngle, String customScaleName);
		//		public int     DAQmxCreateCIGPSTimestampChan     (int taskHandle, String counter, String nameToAssignToChannel, int units, int syncMethod, String customScaleName);
		//
		//		public int     DAQmxCreateCOPulseChanFreq        (int taskHandle, String counter, String nameToAssignToChannel, int units, int idleState, double initialDelay, double freq, double dutyCycle);
		//		public int     DAQmxCreateCOPulseChanTime        (int taskHandle, String counter, String nameToAssignToChannel, int units, int idleState, double initialDelay, double lowTime, double highTime);
		//		public int     DAQmxCreateCOPulseChanTicks       (int taskHandle, String counter, String nameToAssignToChannel, String sourceTerminal, int idleState, int initialDelay, int lowTicks, int highTicks);

		public int     DAQmxGetAIChanCalCalDate(int taskHandle, String channelNames, IntByReference year, IntByReference month, IntByReference day, IntByReference hour, IntByReference minute);
		public int     DAQmxSetAIChanCalCalDate(int taskHandle, String channelNames, int year, int month, int day, int hour, int minute);
		public int     DAQmxGetAIChanCalExpDate(int taskHandle, String channelNames, IntByReference year, IntByReference month, IntByReference day, IntByReference hour, IntByReference minute);
		public int     DAQmxSetAIChanCalExpDate(int taskHandle, String channelNames, int year, int month, int day, int hour, int minute);

		public int   DAQmxGetChanAttribute             (int taskHandle, String channel, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetChanAttribute             (int taskHandle, String channel, int attribute, ByReference... args);
		public int     DAQmxResetChanAttribute           (int taskHandle, String channel, int attribute);


		/******************************************************/
		/***                    Timing                      ***/
		/******************************************************/


		// (Analog/Counter Timing)
		public int     DAQmxCfgSampClkTiming          (int taskHandle, String source, double rate, int activeEdge, int sampleMode, long sampsPerChan);
		// (Digital Timing)
		public int     DAQmxCfgHandshakingTiming      (int taskHandle, int sampleMode, long sampsPerChan);
		// (Burst Import Clock Timing)
		public int     DAQmxCfgBurstHandshakingTimingImportClock(int taskHandle, int sampleMode, long sampsPerChan, double sampleClkRate, String sampleClkSrc, int sampleClkActiveEdge, int pauseWhen, int readyEventActiveLevel);
		// (Burst Export Clock Timing)
		public int     DAQmxCfgBurstHandshakingTimingExportClock(int taskHandle, int sampleMode, long sampsPerChan, double sampleClkRate, String sampleClkOutpTerm, int sampleClkPulsePolarity, int pauseWhen, int readyEventActiveLevel);
		public int     DAQmxCfgChangeDetectionTiming  (int taskHandle, String risingEdgeChan, String fallingEdgeChan, int sampleMode, long sampsPerChan);
		// (Counter Timing)
		public int     DAQmxCfgImplicitTiming         (int taskHandle, int sampleMode, long sampsPerChan);
		// (Pipelined Sample Clock Timing)
		public int     DAQmxCfgPipelinedSampClkTiming (int taskHandle, String source, double rate, int activeEdge, int sampleMode, long sampsPerChan);

		public int   DAQmxGetTimingAttribute        (int taskHandle, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetTimingAttribute        (int taskHandle, int attribute, ByReference... args);
		public int     DAQmxResetTimingAttribute      (int taskHandle, int attribute);

		public int   DAQmxGetTimingAttributeEx      (int taskHandle, String deviceNames, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetTimingAttributeEx      (int taskHandle, String deviceNames, int attribute, ByReference... args);
		public int     DAQmxResetTimingAttributeEx    (int taskHandle, String deviceNames, int attribute);


		/******************************************************/
		/***                  Triggering                    ***/
		/******************************************************/


		public int     DAQmxDisableStartTrig      (int taskHandle);
		public int     DAQmxCfgDigEdgeStartTrig   (int taskHandle, String triggerSource, int triggerEdge);
		public int     DAQmxCfgAnlgEdgeStartTrig  (int taskHandle, String triggerSource, int triggerSlope, double triggerLevel);
		public int     DAQmxCfgAnlgWindowStartTrig(int taskHandle, String triggerSource, int triggerWhen, double windowTop, double windowBottom);
		public int     DAQmxCfgDigPatternStartTrig(int taskHandle, String triggerSource, String triggerPattern, int triggerWhen);

		public int     DAQmxDisableRefTrig        (int taskHandle);
		public int     DAQmxCfgDigEdgeRefTrig     (int taskHandle, String triggerSource, int triggerEdge, int pretriggerSamples);
		public int     DAQmxCfgAnlgEdgeRefTrig    (int taskHandle, String triggerSource, int triggerSlope, double triggerLevel, int pretriggerSamples);
		public int     DAQmxCfgAnlgWindowRefTrig  (int taskHandle, String triggerSource, int triggerWhen, double windowTop, double windowBottom, int pretriggerSamples);
		public int     DAQmxCfgDigPatternRefTrig  (int taskHandle, String triggerSource, String triggerPattern, int triggerWhen, int pretriggerSamples);

		public int     DAQmxDisableAdvTrig        (int taskHandle);
		public int     DAQmxCfgDigEdgeAdvTrig     (int taskHandle, String triggerSource, int triggerEdge);

		public int   DAQmxGetTrigAttribute      (int taskHandle, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetTrigAttribute      (int taskHandle, int attribute, ByReference... args);
		public int     DAQmxResetTrigAttribute    (int taskHandle, int attribute);

		public int     DAQmxSendSoftwareTrigger   (int taskHandle, int triggerID);


		/******************************************************/
		/***                 Read Data                      ***/
		/******************************************************/


		public int     DAQmxReadAnalogF64         (int taskHandle, int numSampsPerChan, double timeout, int fillMode, double readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		public int     DAQmxReadAnalogScalarF64   (int taskHandle, double timeout, DoubleByReference value, IntByReference reserved);

		public int     DAQmxReadBinaryI16         (int taskHandle, int numSampsPerChan, double timeout, int fillMode, short readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);

		public int     DAQmxReadBinaryU16         (int taskHandle, int numSampsPerChan, double timeout, int fillMode, short readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);

		public int     DAQmxReadBinaryI32         (int taskHandle, int numSampsPerChan, double timeout, int fillMode, int readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);

		public int     DAQmxReadBinaryU32         (int taskHandle, int numSampsPerChan, double timeout, int fillMode, int readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);

		public int     DAQmxReadDigitalU8         (int taskHandle, int numSampsPerChan, double timeout, int fillMode, byte readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		public int     DAQmxReadDigitalU16        (int taskHandle, int numSampsPerChan, double timeout, int fillMode, short readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		public int     DAQmxReadDigitalU32        (int taskHandle, int numSampsPerChan, double timeout, int fillMode, int readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		public int     DAQmxReadDigitalScalarU32  (int taskHandle, double timeout, IntByReference value, IntByReference reserved);
		public int	    DAQmxReadDigitalLines      (int taskHandle, int numSampsPerChan, double timeout, int fillMode, byte readArray[], int arraySizeInBytes, IntByReference sampsPerChanRead, IntByReference numBytesPerSamp, IntByReference reserved);

		public int     DAQmxReadCounterF64        (int taskHandle, int numSampsPerChan, double timeout, double readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		public int     DAQmxReadCounterU32        (int taskHandle, int numSampsPerChan, double timeout, int readArray[], int arraySizeInSamps, IntByReference sampsPerChanRead, IntByReference reserved);
		public int     DAQmxReadCounterScalarF64  (int taskHandle, double timeout, DoubleByReference value, IntByReference reserved);
		public int     DAQmxReadCounterScalarU32  (int taskHandle, double timeout, IntByReference value, IntByReference reserved);

		public int     DAQmxReadRaw               (int taskHandle, int numSampsPerChan, double timeout, ByReference readArray, int arraySizeInBytes, IntByReference sampsRead, IntByReference numBytesPerSamp, IntByReference reserved);

		public int     DAQmxGetNthTaskReadChannel (int taskHandle, int index, byte buffer[], int bufferSize);

		public int   DAQmxGetReadAttribute      (int taskHandle, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetReadAttribute      (int taskHandle, int attribute, ByReference... args);
		public int     DAQmxResetReadAttribute    (int taskHandle, int attribute);


		/******************************************************/
		/***                 Write Data                     ***/
		/******************************************************/


		public int     DAQmxWriteAnalogF64          (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final double writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteAnalogScalarF64    (int taskHandle, int autoStart, double timeout, double value, IntByReference reserved);

		public int     DAQmxWriteBinaryI16          (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final short writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteBinaryU16          (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final short writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteBinaryI32          (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final int writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteBinaryU32          (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final int writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);

		public int     DAQmxWriteDigitalU8          (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final byte writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteDigitalU16         (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final short writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteDigitalU32         (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final int writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteDigitalScalarU32   (int taskHandle, int autoStart, double timeout, int value, IntByReference reserved);
		public int     DAQmxWriteDigitalLines       (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final byte writeArray[], IntByReference sampsPerChanWritten, IntByReference reserved);

		public int     DAQmxWriteCtrFreq            (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final double frequency[], final double dutyCycle[], IntByReference numSampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteCtrFreqScalar      (int taskHandle, int autoStart, double timeout, double frequency, double dutyCycle, IntByReference reserved);
		public int     DAQmxWriteCtrTime            (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final double highTime[], final double lowTime[], IntByReference numSampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteCtrTimeScalar      (int taskHandle, int autoStart, double timeout, double highTime, double lowTime, IntByReference reserved);
		public int     DAQmxWriteCtrTicks           (int taskHandle, int numSampsPerChan, int autoStart, double timeout, int dataLayout, final int highTicks[], final int lowTicks[], IntByReference numSampsPerChanWritten, IntByReference reserved);
		public int     DAQmxWriteCtrTicksScalar     (int taskHandle, int autoStart, double timeout, int highTicks, int lowTicks, IntByReference reserved);

		public int     DAQmxWriteRaw                (int taskHandle, int numSamps, int autoStart, double timeout, ByReference writeArray, IntByReference sampsPerChanWritten, IntByReference reserved);

		public int   DAQmxGetWriteAttribute       (int taskHandle, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetWriteAttribute       (int taskHandle, int attribute, ByReference... args);
		public int     DAQmxResetWriteAttribute     (int taskHandle, int attribute);


		/******************************************************/
		/***               Events & Signals                 ***/
		/******************************************************/

		// Terminology:  For hardware, "signals" comprise "clocks," "triggers," and (output) "events".
		// Software signals or events are not presently supported.

		// For possible values for parameter signalID see value set Signal in Values section above.
		public int     DAQmxExportSignal                (int taskHandle, int signalID, String outputTerminal);

		public int   DAQmxGetExportedSignalAttribute  (int taskHandle, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetExportedSignalAttribute  (int taskHandle, int attribute, ByReference... args);
		public int     DAQmxResetExportedSignalAttribute(int taskHandle, int attribute);


		/******************************************************/
		/***              Scale Configurations              ***/
		/******************************************************/


		public int     DAQmxCreateLinScale             (String name, double slope, double yIntercept, int preScaledUnits, String scaledUnits);
		public int     DAQmxCreateMapScale             (String name, double prescaledMin, double prescaledMax, double scaledMin, double scaledMax, int preScaledUnits, String scaledUnits);
		public int     DAQmxCreatePolynomialScale      (String name, final double forwardCoeffs[], int numForwardCoeffsIn, final double reverseCoeffs[], int numReverseCoeffsIn, int preScaledUnits, String scaledUnits);
		public int     DAQmxCreateTableScale           (String name, final double prescaledVals[], int numPrescaledValsIn, final double scaledVals[], int numScaledValsIn, int preScaledUnits, String scaledUnits);
		public int     DAQmxCalculateReversePolyCoeff  (final double forwardCoeffs[], int numForwardCoeffsIn, double minValX, double maxValX, int numPointsToCompute, int reversePolyOrder, double reverseCoeffs[]);

		public int   DAQmxGetScaleAttribute          (String scaleName, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetScaleAttribute          (String scaleName, int attribute, ByReference... args);


		/******************************************************/
		/***             Buffer Configurations              ***/
		/******************************************************/


		public int     DAQmxCfgInputBuffer      (int taskHandle, int numSampsPerChan);
		public int     DAQmxCfgOutputBuffer     (int taskHandle, int numSampsPerChan);

		public int   DAQmxGetBufferAttribute  (int taskHandle, int attribute, ByReference value);
		public int   DAQmxSetBufferAttribute  (int taskHandle, int attribute, ByReference... args);
		public int     DAQmxResetBufferAttribute(int taskHandle, int attribute);

		//		/******************************************************/
		//		/***                Switch Functions                ***/
		//		/******************************************************/
		//
		//
		//		public int     DAQmxSwitchCreateScanList      (String scanList, LongByReference taskHandle);
		//
		//		public int     DAQmxSwitchConnect             (String switchChannel1, String switchChannel2, int waitForSettling);
		//		public int     DAQmxSwitchConnectMulti        (String connectionListconnectionList, int waitForSettling);
		//		public int     DAQmxSwitchDisconnect          (String switchChannel1, String switchChannel2, int waitForSettling);
		//		public int     DAQmxSwitchDisconnectMulti     (String connectionListconnectionList, int waitForSettling);
		//		public int     DAQmxSwitchDisconnectAll       (String deviceName, int waitForSettling);
		//
		//
		//		public int     DAQmxSwitchSetTopologyAndReset (String deviceName, String newTopology);
		//
		//		// For possible values of the output parameter pathStatus see value set SwitchPathType in Values section above.
		//		public int     DAQmxSwitchFindPath            (String switchChannel1, String switchChannel2, byte path[], int pathBufferSize, IntByReference pathStatus);
		//
		//		public int     DAQmxSwitchOpenRelays          (String relayList, int waitForSettling);
		//		public int     DAQmxSwitchCloseRelays         (String relayList, int waitForSettling);
		//
		//		public int     DAQmxSwitchGetSingleRelayCount (String relayName, IntByReference count);
		//		public int     DAQmxSwitchGetMultiRelayCount  (String relayList, int count[], int countArraySize, IntByReference numRelayCountsRead);
		//		// For possible values of the output parameter relayPos see value set RelayPos in Values section above.
		//		public int     DAQmxSwitchGetSingleRelayPos   (String relayName, IntByReference relayPos);
		//		// For possible values in the output array relayPos see value set RelayPos in Values section above.
		//		public int     DAQmxSwitchGetMultiRelayPos    (String relayList, int relayPos[], int relayPosArraySize, IntByReference numRelayPossRead);
		//
		//		public int     DAQmxSwitchWaitForSettling     (String deviceName);
		//
		//		public int   DAQmxGetSwitchChanAttribute    (String switchChannelName, int attribute, ByReference value);
		//		public int   DAQmxSetSwitchChanAttribute    (String switchChannelName, int attribute, ByReference... args);
		//
		//		public int   DAQmxGetSwitchDeviceAttribute  (String deviceName, int attribute, ByReference value, ByReference... args);
		//		public int   DAQmxSetSwitchDeviceAttribute  (String deviceName, int attribute, ByReference... args);
		//
		//		public int   DAQmxGetSwitchScanAttribute    (int taskHandle, int attribute, ByReference value);
		//		public int   DAQmxSetSwitchScanAttribute    (int taskHandle, int attribute, ByReference... args);
		//		public int     DAQmxResetSwitchScanAttribute  (int taskHandle, int attribute);
		//
		//
		//		/******************************************************/
		//		/***                Signal Routing                  ***/
		//		/******************************************************/
		//
		//
		//		public int     DAQmxConnectTerms         (String sourceTerminal, String destinationTerminal, int signalModifiers);
		//		public int     DAQmxDisconnectTerms      (String sourceTerminal, String destinationTerminal);
		//		public int     DAQmxTristateOutputTerm   (String outputTerminal);


		/******************************************************/
		/***                Device Control                  ***/
		/******************************************************/


		public int     DAQmxResetDevice              (String deviceName);

		public int   DAQmxGetDeviceAttribute       (String deviceName, int attribute, ByReference value, ByReference... args);

		/******************************************************/
		/***              Watchdog Timer                    ***/
		/******************************************************/


		public int   DAQmxCreateWatchdogTimerTask    (String deviceName, String taskName, LongByReference taskHandle, double timeout, String lines, int expState, ByReference... args);
		public int     DAQmxControlWatchdogTask        (int taskHandle, int action);

		public int  DAQmxGetWatchdogAttribute        (int taskHandle, String lines, int attribute, ByReference value, ByReference... args);
		public int  DAQmxSetWatchdogAttribute        (int taskHandle, String lines, int attribute, ByReference... args);
		public int    DAQmxResetWatchdogAttribute      (int taskHandle, String lines, int attribute);


		//		/******************************************************/
		//		/***                 Calibration                    ***/
		//		/******************************************************/
		//
		//
		//		public int     DAQmxSelfCal                    (String deviceName);
		//		public int     DAQmxPerformBridgeOffsetNullingCal(int taskHandle, String channel);
		//		public int     DAQmxPerformBridgeOffsetNullingCalEx(int taskHandle, String channel, int skipUnsupportedChannels);
		//		public int     DAQmxPerformStrainShuntCal      (int taskHandle, String channel, double shuntResistorValue, int shuntResistorLocation, int skipUnsupportedChannels);
		//		public int     DAQmxPerformBridgeShuntCal      (int taskHandle, String channel, double shuntResistorValue, int shuntResistorLocation, double bridgeResistance, int skipUnsupportedChannels);
		//		public int     DAQmxGetSelfCalLastDateAndTime  (String deviceName, IntByReference year, IntByReference month, IntByReference day, IntByReference hour, IntByReference minute);
		//		public int     DAQmxGetExtCalLastDateAndTime   (String deviceName, IntByReference year, IntByReference month, IntByReference day, IntByReference hour, IntByReference minute);
		//		public int     DAQmxRestoreLastExtCalConst     (String deviceName);
		//
		//		public int     DAQmxESeriesCalAdjust           (int calHandle, double referenceVoltage);
		//		public int     DAQmxMSeriesCalAdjust           (int calHandle, double referenceVoltage);
		//		public int     DAQmxSSeriesCalAdjust           (int calHandle, double referenceVoltage);
		//		public int     DAQmxSCBaseboardCalAdjust       (int calHandle, double referenceVoltage);
		//		public int     DAQmxAOSeriesCalAdjust          (int calHandle, double referenceVoltage);
		//
		//		public int     DAQmxDeviceSupportsCal          (String deviceName, IntByReference calSupported);
		//
		//		public int   DAQmxGetCalInfoAttribute        (String deviceName, int attribute, ByReference value, ByReference... args);
		//		public int   DAQmxSetCalInfoAttribute        (String deviceName, int attribute, ByReference... args);
		//
		//		public int     DAQmxInitExtCal                 (String deviceName, String password, IntByReference calHandle);
		//		public int     DAQmxCloseExtCal                (int calHandle, int action);
		//		public int     DAQmxChangeExtCalPassword       (String deviceName, String password, String newPassword);
		//
		//		public int     DAQmxAdjustDSAAICal             (int calHandle, double referenceVoltage);
		//		public int     DAQmxAdjustDSAAOCal             (int calHandle, int channel, double requestedLowVoltage, double actualLowVoltage, double requestedHighVoltage, double actualHighVoltage, double gainSetting);
		//		public int     DAQmxAdjustDSATimebaseCal       (int calHandle, double referenceFrequency);
		//
		//		public int     DAQmxAdjust4204Cal              (int calHandle, String channelNames, double lowPassFreq, int trackHoldEnabled, double inputVal);
		//		public int     DAQmxAdjust4220Cal              (int calHandle, String channelNames, double gain, double inputVal);
		//		public int     DAQmxAdjust4224Cal              (int calHandle, String channelNames, double gain, double inputVal);
		//		// Note: This function is obsolete and now always returns zero.
		//		public int     DAQmxAdjust4225Cal              (int calHandle, String channelNames, double gain, double inputVal);
		//
		//		public int     DAQmxSetup1102Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1102Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1104Cal               (int calHandle, String channelNames);
		//		public int     DAQmxAdjust1104Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1112Cal               (int calHandle, String channelNames);
		//		public int     DAQmxAdjust1112Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1122Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1122Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1124Cal               (int calHandle, String channelNames, int range, int dacValue);
		//		public int     DAQmxAdjust1124Cal              (int calHandle, double measOutput);
		//
		//		public int     DAQmxSetup1125Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1125Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1126Cal               (int calHandle, String channelNames, double upperFreqLimit);
		//		public int     DAQmxAdjust1126Cal              (int calHandle, double refFreq, double measOutput);
		//
		//		public int     DAQmxSetup1141Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1141Cal              (int calHandle, double refVoltage, double measOutput);
		//		public int     DAQmxSetup1142Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1142Cal              (int calHandle, double refVoltage, double measOutput);
		//		public int     DAQmxSetup1143Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1143Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//
		//		public int     DAQmxSetup1502Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1502Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1503Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1503Cal              (int calHandle, double refVoltage, double measOutput);
		//		public int     DAQmxAdjust1503CurrentCal       (int calHandle, String channelNames, double measCurrent);
		//
		//		public int     DAQmxSetup1520Cal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust1520Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1521Cal               (int calHandle, String channelNames);
		//		public int     DAQmxAdjust1521Cal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup153xCal               (int calHandle, String channelNames, double gain);
		//		public int     DAQmxAdjust153xCal              (int calHandle, double refVoltage, double measOutput);
		//
		//		public int     DAQmxSetup1540Cal               (int calHandle, String channelNames, double excitationVoltage, double excitationFreq);
		//		public int     DAQmxAdjust1540Cal              (int calHandle, double refVoltage, double measOutput, int inputCalSource);
		//
		//
		//		/******************************************************/
		//		/***                     TEDS                       ***/
		//		/******************************************************/
		//
		//		public int     DAQmxConfigureTEDS              (String physicalChannel, String filePath);
		//		public int     DAQmxClearTEDS                  (String physicalChannel);
		//		public int     DAQmxWriteToTEDSFromArray       (String physicalChannel, final byte[] bitStream, int arraySize, int basicTEDSOptions);
		//		public int     DAQmxWriteToTEDSFromFile        (String physicalChannel, String filePath, int basicTEDSOptions);
		//		public int   DAQmxGetPhysicalChanAttribute   (String physicalChannel, int attribute, ByReference value, ByReference... args);


		/******************************************************/
		/***                  Real-Time                     ***/
		/******************************************************/

		public int     DAQmxWaitForNextSampleClock(int taskHandle, double timeout, IntByReference isLate);
		public int   DAQmxGetRealTimeAttribute  (int taskHandle, int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetRealTimeAttribute  (int taskHandle, int attribute, ByReference... args);
		public int     DAQmxResetRealTimeAttribute(int taskHandle, int attribute);

		// Note: This function is obsolete and now always returns zero.
		public int   DAQmxIsReadOrWriteLate     (int errorCode);


		//		/******************************************************/
		//		/***                   Storage                      ***/
		//		/******************************************************/
		//
		//		public int     DAQmxSaveTask                    (int taskHandle, String saveAs, String author, int options);
		//		public int     DAQmxSaveGlobalChan              (int taskHandle, String channelNames, String saveAs, String author, int options);
		//		public int     DAQmxSaveScale                   (String scaleName, String saveAs, String author, int options);
		//		public int     DAQmxDeleteSavedTask             (String taskName);
		//		public int     DAQmxDeleteSavedGlobalChan       (String channelNames);
		//		public int     DAQmxDeleteSavedScale            (String scaleName);
		//
		//		public int   DAQmxGetPersistedTaskAttribute   (String taskName, int attribute, ByReference value, ByReference... args);
		//		public int   DAQmxGetPersistedChanAttribute   (String channel, int attribute, ByReference value, ByReference... args);
		//		public int   DAQmxGetPersistedScaleAttribute  (String scaleName, int attribute, ByReference value, ByReference... args);


		/******************************************************/
		/***              System Configuration              ***/
		/******************************************************/

		public int   DAQmxGetSystemInfoAttribute (int attribute, ByReference value, ByReference... args);
		public int   DAQmxSetDigitalPowerUpStates(String deviceName, String channelNames, int state, ByReference... args);
		public int   DAQmxSetAnalogPowerUpStates(String deviceName, String channelNames, double state, int channelType, ByReference... args);
		public int     DAQmxSetDigitalLogicFamilyPowerUpState(String deviceName, int logicFamily);

		/******************************************************/
		/***                 Error Handling                 ***/
		/******************************************************/


		public int     DAQmxGetErrorString       (int errorCode, byte errorString[], int bufferSize);
		public int     DAQmxGetExtendedErrorInfo (char errorString[], int bufferSize);


		/******************************************************************************
		 *** NI-DAQmx Specific Attribute Get/Set/Reset Function Declarations **********
		 ******************************************************************************/

		//********** Buffer **********
		//*** Set/Get functions for DAQmx_Buf_Input_BufSize ***
		public int DAQmxGetBufInputBufSize(int taskHandle, IntByReference data);
		public int DAQmxSetBufInputBufSize(int taskHandle, int data);
		public int DAQmxResetBufInputBufSize(int taskHandle);
		//*** Set/Get functions for DAQmx_Buf_Input_OnbrdBufSize ***
		public int DAQmxGetBufInputOnbrdBufSize(int taskHandle, IntByReference data);
		//*** Set/Get functions for DAQmx_Buf_Output_BufSize ***
		public int DAQmxGetBufOutputBufSize(int taskHandle, IntByReference data);
		public int DAQmxSetBufOutputBufSize(int taskHandle, int data);
		public int DAQmxResetBufOutputBufSize(int taskHandle);
		//*** Set/Get functions for DAQmx_Buf_Output_OnbrdBufSize ***
		public int DAQmxGetBufOutputOnbrdBufSize(int taskHandle, IntByReference data);
		public int DAQmxSetBufOutputOnbrdBufSize(int taskHandle, int data);
		public int DAQmxResetBufOutputOnbrdBufSize(int taskHandle);

		//********** Calibration Info **********
		//*** Set/Get functions for DAQmx_SelfCal_Supported ***
		public int DAQmxGetSelfCalSupported(String deviceName, IntByReference data);
		//*** Set/Get functions for DAQmx_SelfCal_LastTemp ***
		public int DAQmxGetSelfCalLastTemp(String deviceName, DoubleByReference data);
		//*** Set/Get functions for DAQmx_ExtCal_RecommendedInterval ***
		public int DAQmxGetExtCalRecommendedInterval(String deviceName, IntByReference data);
		//*** Set/Get functions for DAQmx_ExtCal_LastTemp ***
		public int DAQmxGetExtCalLastTemp(String deviceName, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Cal_UserDefinedInfo ***
		public int DAQmxGetCalUserDefinedInfo(String deviceName, byte[] data, int bufferSize);
		public int DAQmxSetCalUserDefinedInfo(String deviceName, final byte[] data);
		//*** Set/Get functions for DAQmx_Cal_UserDefinedInfo_MaxSize ***
		public int DAQmxGetCalUserDefinedInfoMaxSize(String deviceName, IntByReference data);
		//*** Set/Get functions for DAQmx_Cal_DevTemp ***
		public int DAQmxGetCalDevTemp(String deviceName, DoubleByReference data);

		//********** Channel **********
		//*** Set/Get functions for DAQmx_AI_Max ***
		public int DAQmxGetAIMax(int taskHandle, String channel, DoubleByReference data);
		public int DAQmxSetAIMax(int taskHandle, String channel, double data);
		public int DAQmxResetAIMax(int taskHandle, String channel);
		//*** Set/Get functions for DAQmx_AI_Min ***
		public int DAQmxGetAIMin(int taskHandle, String channel, DoubleByReference data);
		public int DAQmxSetAIMin(int taskHandle, String channel, double data);
		public int DAQmxResetAIMin(int taskHandle, String channel);
		//*** Set/Get functions for DAQmx_AI_CustomScaleName ***
		public int DAQmxGetAICustomScaleName(int taskHandle, String channel, byte[] data, int bufferSize);
		public int DAQmxSetAICustomScaleName(int taskHandle, String channel, final byte[] data);
		public int DAQmxResetAICustomScaleName(int taskHandle, String channel);
		//*** Set/Get functions for DAQmx_AI_MeasType ***
		// Uses value set AIMeasurementType
		public int DAQmxGetAIMeasType(int taskHandle, String channel, IntByReference data);
		//*** Set/Get functions for DAQmx_AI_Voltage_Units ***
		// Uses value set VoltageUnits1
		public int DAQmxGetAIVoltageUnits(int taskHandle, String channel, IntByReference data);
		public int DAQmxSetAIVoltageUnits(int taskHandle, String channel, int data);
		public int DAQmxResetAIVoltageUnits(int taskHandle, String channel);
		//*** Set/Get functions for DAQmx_AI_Voltage_dBRef ***
		public int DAQmxGetAIVoltagedBRef(int taskHandle, String channel, DoubleByReference data);
		public int DAQmxSetAIVoltagedBRef(int taskHandle, String channel, double data);
		public int DAQmxResetAIVoltagedBRef(int taskHandle, String channel);
		//*** Set/Get functions for DAQmx_AI_Voltage_ACRMS_Units ***
		// Uses value set VoltageUnits1
		public int DAQmxGetAIVoltageACRMSUnits(int taskHandle, String channel, IntByReference data);
		public int DAQmxSetAIVoltageACRMSUnits(int taskHandle, String channel, int data);
		public int DAQmxResetAIVoltageACRMSUnits(int taskHandle, String channel);
		//*** Set/Get functions for DAQmx_AI_Temp_Units ***
		//		// Uses value set TemperatureUnits1
		//		public int DAQmxGetAITempUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAITempUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAITempUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmcpl_Type ***
		//		// Uses value set ThermocoupleType1
		//		public int DAQmxGetAIThrmcplType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIThrmcplType(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIThrmcplType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmcpl_ScaleType ***
		//		// Uses value set ScaleType2
		//		public int DAQmxGetAIThrmcplScaleType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIThrmcplScaleType(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIThrmcplScaleType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmcpl_CJCSrc ***
		//		// Uses value set CJCSource1
		//		public int DAQmxGetAIThrmcplCJCSrc(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_AI_Thrmcpl_CJCVal ***
		//		public int DAQmxGetAIThrmcplCJCVal(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIThrmcplCJCVal(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIThrmcplCJCVal(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmcpl_CJCChan ***
		//		public int DAQmxGetAIThrmcplCJCChan(int taskHandle, String channel, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_AI_RTD_Type ***
		//		// Uses value set RTDType1
		//		public int DAQmxGetAIRTDType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIRTDType(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIRTDType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RTD_R0 ***
		//		public int DAQmxGetAIRTDR0(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIRTDR0(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIRTDR0(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RTD_A ***
		//		public int DAQmxGetAIRTDA(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIRTDA(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIRTDA(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RTD_B ***
		//		public int DAQmxGetAIRTDB(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIRTDB(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIRTDB(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RTD_C ***
		//		public int DAQmxGetAIRTDC(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIRTDC(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIRTDC(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmstr_A ***
		//		public int DAQmxGetAIThrmstrA(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIThrmstrA(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIThrmstrA(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmstr_B ***
		//		public int DAQmxGetAIThrmstrB(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIThrmstrB(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIThrmstrB(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmstr_C ***
		//		public int DAQmxGetAIThrmstrC(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIThrmstrC(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIThrmstrC(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Thrmstr_R1 ***
		//		public int DAQmxGetAIThrmstrR1(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIThrmstrR1(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIThrmstrR1(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ForceReadFromChan ***
		//		public int DAQmxGetAIForceReadFromChan(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIForceReadFromChan(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIForceReadFromChan(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Current_Units ***
		//		// Uses value set CurrentUnits1
		//		public int DAQmxGetAICurrentUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAICurrentUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAICurrentUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Current_ACRMS_Units ***
		//		// Uses value set CurrentUnits1
		//		public int DAQmxGetAICurrentACRMSUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAICurrentACRMSUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAICurrentACRMSUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Strain_Units ***
		//		// Uses value set StrainUnits1
		//		public int DAQmxGetAIStrainUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIStrainUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIStrainUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_StrainGage_GageFactor ***
		//		public int DAQmxGetAIStrainGageGageFactor(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIStrainGageGageFactor(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIStrainGageGageFactor(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_StrainGage_PoissonRatio ***
		//		public int DAQmxGetAIStrainGagePoissonRatio(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIStrainGagePoissonRatio(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIStrainGagePoissonRatio(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_StrainGage_Cfg ***
		//		// Uses value set StrainGageBridgeType1
		//		public int DAQmxGetAIStrainGageCfg(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIStrainGageCfg(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIStrainGageCfg(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Resistance_Units ***
		//		// Uses value set ResistanceUnits1
		//		public int DAQmxGetAIResistanceUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIResistanceUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIResistanceUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Freq_Units ***
		//		// Uses value set FrequencyUnits
		//		public int DAQmxGetAIFreqUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIFreqUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIFreqUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Freq_ThreshVoltage ***
		//		public int DAQmxGetAIFreqThreshVoltage(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIFreqThreshVoltage(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIFreqThreshVoltage(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Freq_Hyst ***
		//		public int DAQmxGetAIFreqHyst(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIFreqHyst(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIFreqHyst(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_LVDT_Units ***
		//		// Uses value set LengthUnits2
		//		public int DAQmxGetAILVDTUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAILVDTUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAILVDTUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_LVDT_Sensitivity ***
		//		public int DAQmxGetAILVDTSensitivity(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAILVDTSensitivity(int taskHandle, String channel, double data);
		//		public int DAQmxResetAILVDTSensitivity(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_LVDT_SensitivityUnits ***
		//		// Uses value set LVDTSensitivityUnits1
		//		public int DAQmxGetAILVDTSensitivityUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAILVDTSensitivityUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAILVDTSensitivityUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RVDT_Units ***
		//		// Uses value set AngleUnits1
		//		public int DAQmxGetAIRVDTUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIRVDTUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIRVDTUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RVDT_Sensitivity ***
		//		public int DAQmxGetAIRVDTSensitivity(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIRVDTSensitivity(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIRVDTSensitivity(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RVDT_SensitivityUnits ***
		//		// Uses value set RVDTSensitivityUnits1
		//		public int DAQmxGetAIRVDTSensitivityUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIRVDTSensitivityUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIRVDTSensitivityUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_SoundPressure_MaxSoundPressureLvl ***
		//		public int DAQmxGetAISoundPressureMaxSoundPressureLvl(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAISoundPressureMaxSoundPressureLvl(int taskHandle, String channel, double data);
		//		public int DAQmxResetAISoundPressureMaxSoundPressureLvl(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_SoundPressure_Units ***
		//		// Uses value set SoundPressureUnits1
		//		public int DAQmxGetAISoundPressureUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAISoundPressureUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAISoundPressureUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_SoundPressure_dBRef ***
		//		public int DAQmxGetAISoundPressuredBRef(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAISoundPressuredBRef(int taskHandle, String channel, double data);
		//		public int DAQmxResetAISoundPressuredBRef(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Microphone_Sensitivity ***
		//		public int DAQmxGetAIMicrophoneSensitivity(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIMicrophoneSensitivity(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIMicrophoneSensitivity(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Accel_Units ***
		//		// Uses value set AccelUnits2
		//		public int DAQmxGetAIAccelUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIAccelUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIAccelUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Accel_dBRef ***
		//		public int DAQmxGetAIAcceldBRef(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIAcceldBRef(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIAcceldBRef(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Accel_Sensitivity ***
		//		public int DAQmxGetAIAccelSensitivity(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIAccelSensitivity(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIAccelSensitivity(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Accel_SensitivityUnits ***
		//		// Uses value set AccelSensitivityUnits1
		//		public int DAQmxGetAIAccelSensitivityUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIAccelSensitivityUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIAccelSensitivityUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Is_TEDS ***
		//		public int DAQmxGetAIIsTEDS(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_AI_TEDS_Units ***
		//		public int DAQmxGetAITEDSUnits(int taskHandle, String channel, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_AI_Coupling ***
		//		// Uses value set Coupling1
		//		public int DAQmxGetAICoupling(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAICoupling(int taskHandle, String channel, int data);
		//		public int DAQmxResetAICoupling(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Impedance ***
		//		// Uses value set Impedance1
		//		public int DAQmxGetAIImpedance(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIImpedance(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIImpedance(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_TermCfg ***
		//		// Uses value set InputTermCfg
		//		public int DAQmxGetAITermCfg(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAITermCfg(int taskHandle, String channel, int data);
		//		public int DAQmxResetAITermCfg(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_InputSrc ***
		//		public int DAQmxGetAIInputSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetAIInputSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetAIInputSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ResistanceCfg ***
		//		// Uses value set ResistanceConfiguration
		//		public int DAQmxGetAIResistanceCfg(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIResistanceCfg(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIResistanceCfg(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_LeadWireResistance ***
		//		public int DAQmxGetAILeadWireResistance(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAILeadWireResistance(int taskHandle, String channel, double data);
		//		public int DAQmxResetAILeadWireResistance(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_Cfg ***
		//		// Uses value set BridgeConfiguration1
		//		public int DAQmxGetAIBridgeCfg(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIBridgeCfg(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIBridgeCfg(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_NomResistance ***
		//		public int DAQmxGetAIBridgeNomResistance(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIBridgeNomResistance(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIBridgeNomResistance(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_InitialVoltage ***
		//		public int DAQmxGetAIBridgeInitialVoltage(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIBridgeInitialVoltage(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIBridgeInitialVoltage(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_ShuntCal_Enable ***
		//		public int DAQmxGetAIBridgeShuntCalEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIBridgeShuntCalEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIBridgeShuntCalEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_ShuntCal_Select ***
		//		// Uses value set ShuntCalSelect
		//		public int DAQmxGetAIBridgeShuntCalSelect(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIBridgeShuntCalSelect(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIBridgeShuntCalSelect(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_ShuntCal_GainAdjust ***
		//		public int DAQmxGetAIBridgeShuntCalGainAdjust(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIBridgeShuntCalGainAdjust(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIBridgeShuntCalGainAdjust(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_Balance_CoarsePot ***
		//		public int DAQmxGetAIBridgeBalanceCoarsePot(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIBridgeBalanceCoarsePot(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIBridgeBalanceCoarsePot(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Bridge_Balance_FinePot ***
		//		public int DAQmxGetAIBridgeBalanceFinePot(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIBridgeBalanceFinePot(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIBridgeBalanceFinePot(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_CurrentShunt_Loc ***
		//		// Uses value set CurrentShuntResistorLocation1
		//		public int DAQmxGetAICurrentShuntLoc(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAICurrentShuntLoc(int taskHandle, String channel, int data);
		//		public int DAQmxResetAICurrentShuntLoc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_CurrentShunt_Resistance ***
		//		public int DAQmxGetAICurrentShuntResistance(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAICurrentShuntResistance(int taskHandle, String channel, double data);
		//		public int DAQmxResetAICurrentShuntResistance(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Excit_Src ***
		//		// Uses value set ExcitationSource
		//		public int DAQmxGetAIExcitSrc(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIExcitSrc(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIExcitSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Excit_Val ***
		//		public int DAQmxGetAIExcitVal(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIExcitVal(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIExcitVal(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Excit_UseForScaling ***
		//		public int DAQmxGetAIExcitUseForScaling(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIExcitUseForScaling(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIExcitUseForScaling(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Excit_UseMultiplexed ***
		//		public int DAQmxGetAIExcitUseMultiplexed(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIExcitUseMultiplexed(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIExcitUseMultiplexed(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Excit_ActualVal ***
		//		public int DAQmxGetAIExcitActualVal(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIExcitActualVal(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIExcitActualVal(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Excit_DCorAC ***
		//		// Uses value set ExcitationDCorAC
		//		public int DAQmxGetAIExcitDCorAC(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIExcitDCorAC(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIExcitDCorAC(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Excit_VoltageOrCurrent ***
		//		// Uses value set ExcitationVoltageOrCurrent
		//		public int DAQmxGetAIExcitVoltageOrCurrent(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIExcitVoltageOrCurrent(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIExcitVoltageOrCurrent(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ACExcit_Freq ***
		//		public int DAQmxGetAIACExcitFreq(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIACExcitFreq(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIACExcitFreq(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ACExcit_SyncEnable ***
		//		public int DAQmxGetAIACExcitSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIACExcitSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIACExcitSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ACExcit_WireMode ***
		//		// Uses value set ACExcitWireMode
		//		public int DAQmxGetAIACExcitWireMode(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIACExcitWireMode(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIACExcitWireMode(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Atten ***
		//		public int DAQmxGetAIAtten(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIAtten(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIAtten(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ProbeAtten ***
		//		public int DAQmxGetAIProbeAtten(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIProbeAtten(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIProbeAtten(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Lowpass_Enable ***
		//		public int DAQmxGetAILowpassEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAILowpassEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAILowpassEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Lowpass_CutoffFreq ***
		//		public int DAQmxGetAILowpassCutoffFreq(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAILowpassCutoffFreq(int taskHandle, String channel, double data);
		//		public int DAQmxResetAILowpassCutoffFreq(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Lowpass_SwitchCap_ClkSrc ***
		//		// Uses value set SourceSelection
		//		public int DAQmxGetAILowpassSwitchCapClkSrc(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAILowpassSwitchCapClkSrc(int taskHandle, String channel, int data);
		//		public int DAQmxResetAILowpassSwitchCapClkSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Lowpass_SwitchCap_ExtClkFreq ***
		//		public int DAQmxGetAILowpassSwitchCapExtClkFreq(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAILowpassSwitchCapExtClkFreq(int taskHandle, String channel, double data);
		//		public int DAQmxResetAILowpassSwitchCapExtClkFreq(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Lowpass_SwitchCap_ExtClkDiv ***
		//		public int DAQmxGetAILowpassSwitchCapExtClkDiv(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAILowpassSwitchCapExtClkDiv(int taskHandle, String channel, int data);
		//		public int DAQmxResetAILowpassSwitchCapExtClkDiv(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Lowpass_SwitchCap_OutClkDiv ***
		//		public int DAQmxGetAILowpassSwitchCapOutClkDiv(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAILowpassSwitchCapOutClkDiv(int taskHandle, String channel, int data);
		//		public int DAQmxResetAILowpassSwitchCapOutClkDiv(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ResolutionUnits ***
		//		// Uses value set ResolutionType1
		//		public int DAQmxGetAIResolutionUnits(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_AI_Resolution ***
		//		public int DAQmxGetAIResolution(int taskHandle, String channel, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_AI_RawSampSize ***
		//		public int DAQmxGetAIRawSampSize(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_AI_RawSampJustification ***
		//		// Uses value set DataJustification1
		//		public int DAQmxGetAIRawSampJustification(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_AI_ADCTimingMode ***
		//		// Uses value set ADCTimingMode
		//		public int DAQmxGetAIADCTimingMode(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIADCTimingMode(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIADCTimingMode(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Dither_Enable ***
		//		public int DAQmxGetAIDitherEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIDitherEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIDitherEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_HasValidCalInfo ***
		//		public int DAQmxGetAIChanCalHasValidCalInfo(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_EnableCal ***
		//		public int DAQmxGetAIChanCalEnableCal(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIChanCalEnableCal(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIChanCalEnableCal(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_ApplyCalIfExp ***
		//		public int DAQmxGetAIChanCalApplyCalIfExp(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIChanCalApplyCalIfExp(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIChanCalApplyCalIfExp(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_ScaleType ***
		//		// Uses value set ScaleType3
		//		public int DAQmxGetAIChanCalScaleType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIChanCalScaleType(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIChanCalScaleType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_Table_PreScaledVals ***
		//		public int DAQmxGetAIChanCalTablePreScaledVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetAIChanCalTablePreScaledVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxResetAIChanCalTablePreScaledVals(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_Table_ScaledVals ***
		//		public int DAQmxGetAIChanCalTableScaledVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetAIChanCalTableScaledVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxResetAIChanCalTableScaledVals(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_Poly_ForwardCoeff ***
		//		public int DAQmxGetAIChanCalPolyForwardCoeff(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetAIChanCalPolyForwardCoeff(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxResetAIChanCalPolyForwardCoeff(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_Poly_ReverseCoeff ***
		//		public int DAQmxGetAIChanCalPolyReverseCoeff(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetAIChanCalPolyReverseCoeff(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxResetAIChanCalPolyReverseCoeff(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_OperatorName ***
		//		public int DAQmxGetAIChanCalOperatorName(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetAIChanCalOperatorName(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetAIChanCalOperatorName(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_Desc ***
		//		public int DAQmxGetAIChanCalDesc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetAIChanCalDesc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetAIChanCalDesc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_Verif_RefVals ***
		//		public int DAQmxGetAIChanCalVerifRefVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetAIChanCalVerifRefVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxResetAIChanCalVerifRefVals(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_ChanCal_Verif_AcqVals ***
		//		public int DAQmxGetAIChanCalVerifAcqVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetAIChanCalVerifAcqVals(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		public int DAQmxResetAIChanCalVerifAcqVals(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Rng_High ***
		//		public int DAQmxGetAIRngHigh(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIRngHigh(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIRngHigh(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Rng_Low ***
		//		public int DAQmxGetAIRngLow(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIRngLow(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIRngLow(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_DCOffset ***
		//		public int DAQmxGetAIDCOffset(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIDCOffset(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIDCOffset(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_Gain ***
		//		public int DAQmxGetAIGain(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAIGain(int taskHandle, String channel, double data);
		//		public int DAQmxResetAIGain(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_SampAndHold_Enable ***
		//		public int DAQmxGetAISampAndHoldEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAISampAndHoldEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAISampAndHoldEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_AutoZeroMode ***
		//		// Uses value set AutoZeroType1
		//		public int DAQmxGetAIAutoZeroMode(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIAutoZeroMode(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIAutoZeroMode(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_DataXferMech ***
		//		// Uses value set DataTransferMechanism
		//		public int DAQmxGetAIDataXferMech(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIDataXferMech(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIDataXferMech(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_DataXferReqCond ***
		//		// Uses value set InputDataTransferCondition
		//		public int DAQmxGetAIDataXferReqCond(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIDataXferReqCond(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIDataXferReqCond(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_DataXferCustomThreshold ***
		//		public int DAQmxGetAIDataXferCustomThreshold(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIDataXferCustomThreshold(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIDataXferCustomThreshold(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_UsbXferReqSize ***
		//		public int DAQmxGetAIUsbXferReqSize(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIUsbXferReqSize(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIUsbXferReqSize(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_MemMapEnable ***
		//		public int DAQmxGetAIMemMapEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIMemMapEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIMemMapEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_RawDataCompressionType ***
		//		// Uses value set RawDataCompressionType
		//		public int DAQmxGetAIRawDataCompressionType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIRawDataCompressionType(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIRawDataCompressionType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_LossyLSBRemoval_CompressedSampSize ***
		//		public int DAQmxGetAILossyLSBRemovalCompressedSampSize(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAILossyLSBRemovalCompressedSampSize(int taskHandle, String channel, int data);
		//		public int DAQmxResetAILossyLSBRemovalCompressedSampSize(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AI_DevScalingCoeff ***
		//		public int DAQmxGetAIDevScalingCoeff(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		//*** Set/Get functions for DAQmx_AI_EnhancedAliasRejectionEnable ***
		//		public int DAQmxGetAIEnhancedAliasRejectionEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAIEnhancedAliasRejectionEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAIEnhancedAliasRejectionEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_Max ***
		//		public int DAQmxGetAOMax(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOMax(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOMax(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_Min ***
		//		public int DAQmxGetAOMin(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOMin(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOMin(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_CustomScaleName ***
		//		public int DAQmxGetAOCustomScaleName(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetAOCustomScaleName(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetAOCustomScaleName(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_OutputType ***
		//		// Uses value set AOOutputChannelType
		//		public int DAQmxGetAOOutputType(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_AO_Voltage_Units ***
		//		// Uses value set VoltageUnits2
		//		public int DAQmxGetAOVoltageUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOVoltageUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOVoltageUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_Voltage_CurrentLimit ***
		//		public int DAQmxGetAOVoltageCurrentLimit(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOVoltageCurrentLimit(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOVoltageCurrentLimit(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_Current_Units ***
		//		// Uses value set CurrentUnits1
		//		public int DAQmxGetAOCurrentUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOCurrentUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOCurrentUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_FuncGen_Type ***
		//		// Uses value set FuncGenType
		//		public int DAQmxGetAOFuncGenType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOFuncGenType(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOFuncGenType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_FuncGen_Freq ***
		//		public int DAQmxGetAOFuncGenFreq(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOFuncGenFreq(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOFuncGenFreq(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_FuncGen_Amplitude ***
		//		public int DAQmxGetAOFuncGenAmplitude(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOFuncGenAmplitude(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOFuncGenAmplitude(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_FuncGen_Offset ***
		//		public int DAQmxGetAOFuncGenOffset(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOFuncGenOffset(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOFuncGenOffset(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_FuncGen_Square_DutyCycle ***
		//		public int DAQmxGetAOFuncGenSquareDutyCycle(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOFuncGenSquareDutyCycle(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOFuncGenSquareDutyCycle(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_FuncGen_ModulationType ***
		//		// Uses value set ModulationType
		//		public int DAQmxGetAOFuncGenModulationType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOFuncGenModulationType(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOFuncGenModulationType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_FuncGen_FMDeviation ***
		//		public int DAQmxGetAOFuncGenFMDeviation(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOFuncGenFMDeviation(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOFuncGenFMDeviation(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_OutputImpedance ***
		//		public int DAQmxGetAOOutputImpedance(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOOutputImpedance(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOOutputImpedance(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_LoadImpedance ***
		//		public int DAQmxGetAOLoadImpedance(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOLoadImpedance(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOLoadImpedance(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_IdleOutputBehavior ***
		//		// Uses value set AOIdleOutputBehavior
		//		public int DAQmxGetAOIdleOutputBehavior(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOIdleOutputBehavior(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOIdleOutputBehavior(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_TermCfg ***
		//		// Uses value set OutputTermCfg
		//		public int DAQmxGetAOTermCfg(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOTermCfg(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOTermCfg(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_ResolutionUnits ***
		//		// Uses value set ResolutionType1
		//		public int DAQmxGetAOResolutionUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOResolutionUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOResolutionUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_Resolution ***
		//		public int DAQmxGetAOResolution(int taskHandle, String channel, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Rng_High ***
		//		public int DAQmxGetAODACRngHigh(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAODACRngHigh(int taskHandle, String channel, double data);
		//		public int DAQmxResetAODACRngHigh(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Rng_Low ***
		//		public int DAQmxGetAODACRngLow(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAODACRngLow(int taskHandle, String channel, double data);
		//		public int DAQmxResetAODACRngLow(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Ref_ConnToGnd ***
		//		public int DAQmxGetAODACRefConnToGnd(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAODACRefConnToGnd(int taskHandle, String channel, int data);
		//		public int DAQmxResetAODACRefConnToGnd(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Ref_AllowConnToGnd ***
		//		public int DAQmxGetAODACRefAllowConnToGnd(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAODACRefAllowConnToGnd(int taskHandle, String channel, int data);
		//		public int DAQmxResetAODACRefAllowConnToGnd(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Ref_Src ***
		//		// Uses value set SourceSelection
		//		public int DAQmxGetAODACRefSrc(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAODACRefSrc(int taskHandle, String channel, int data);
		//		public int DAQmxResetAODACRefSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Ref_ExtSrc ***
		//		public int DAQmxGetAODACRefExtSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetAODACRefExtSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetAODACRefExtSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Ref_Val ***
		//		public int DAQmxGetAODACRefVal(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAODACRefVal(int taskHandle, String channel, double data);
		//		public int DAQmxResetAODACRefVal(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Offset_Src ***
		//		// Uses value set SourceSelection
		//		public int DAQmxGetAODACOffsetSrc(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAODACOffsetSrc(int taskHandle, String channel, int data);
		//		public int DAQmxResetAODACOffsetSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Offset_ExtSrc ***
		//		public int DAQmxGetAODACOffsetExtSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetAODACOffsetExtSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetAODACOffsetExtSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DAC_Offset_Val ***
		//		public int DAQmxGetAODACOffsetVal(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAODACOffsetVal(int taskHandle, String channel, double data);
		//		public int DAQmxResetAODACOffsetVal(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_ReglitchEnable ***
		//		public int DAQmxGetAOReglitchEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOReglitchEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOReglitchEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_Gain ***
		//		public int DAQmxGetAOGain(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetAOGain(int taskHandle, String channel, double data);
		//		public int DAQmxResetAOGain(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_UseOnlyOnBrdMem ***
		//		public int DAQmxGetAOUseOnlyOnBrdMem(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOUseOnlyOnBrdMem(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOUseOnlyOnBrdMem(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DataXferMech ***
		//		// Uses value set DataTransferMechanism
		//		public int DAQmxGetAODataXferMech(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAODataXferMech(int taskHandle, String channel, int data);
		//		public int DAQmxResetAODataXferMech(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DataXferReqCond ***
		//		// Uses value set OutputDataTransferCondition
		//		public int DAQmxGetAODataXferReqCond(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAODataXferReqCond(int taskHandle, String channel, int data);
		//		public int DAQmxResetAODataXferReqCond(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_UsbXferReqSize ***
		//		public int DAQmxGetAOUsbXferReqSize(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOUsbXferReqSize(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOUsbXferReqSize(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_MemMapEnable ***
		//		public int DAQmxGetAOMemMapEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOMemMapEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOMemMapEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_AO_DevScalingCoeff ***
		//		public int DAQmxGetAODevScalingCoeff(int taskHandle, String channel, double[] data, int arraySizeInSamples);
		//		//*** Set/Get functions for DAQmx_AO_EnhancedImageRejectionEnable ***
		//		public int DAQmxGetAOEnhancedImageRejectionEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetAOEnhancedImageRejectionEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetAOEnhancedImageRejectionEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_InvertLines ***
		//		public int DAQmxGetDIInvertLines(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDIInvertLines(int taskHandle, String channel, int data);
		//		public int DAQmxResetDIInvertLines(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_NumLines ***
		//		public int DAQmxGetDINumLines(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_DI_DigFltr_Enable ***
		//		public int DAQmxGetDIDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDIDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetDIDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetDIDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetDIDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetDIDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_Tristate ***
		//		public int DAQmxGetDITristate(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDITristate(int taskHandle, String channel, int data);
		//		public int DAQmxResetDITristate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_LogicFamily ***
		//		// Uses value set LogicFamily
		//		public int DAQmxGetDILogicFamily(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDILogicFamily(int taskHandle, String channel, int data);
		//		public int DAQmxResetDILogicFamily(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_DataXferMech ***
		//		// Uses value set DataTransferMechanism
		//		public int DAQmxGetDIDataXferMech(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDIDataXferMech(int taskHandle, String channel, int data);
		//		public int DAQmxResetDIDataXferMech(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_DataXferReqCond ***
		//		// Uses value set InputDataTransferCondition
		//		public int DAQmxGetDIDataXferReqCond(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDIDataXferReqCond(int taskHandle, String channel, int data);
		//		public int DAQmxResetDIDataXferReqCond(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_UsbXferReqSize ***
		//		public int DAQmxGetDIUsbXferReqSize(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDIUsbXferReqSize(int taskHandle, String channel, int data);
		//		public int DAQmxResetDIUsbXferReqSize(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_MemMapEnable ***
		//		public int DAQmxGetDIMemMapEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDIMemMapEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetDIMemMapEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DI_AcquireOn ***
		//		// Uses value set SampleClockActiveOrInactiveEdgeSelection
		//		public int DAQmxGetDIAcquireOn(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDIAcquireOn(int taskHandle, String channel, int data);
		//		public int DAQmxResetDIAcquireOn(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_OutputDriveType ***
		//		// Uses value set DigitalDriveType
		//		public int DAQmxGetDOOutputDriveType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOOutputDriveType(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOOutputDriveType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_InvertLines ***
		//		public int DAQmxGetDOInvertLines(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOInvertLines(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOInvertLines(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_NumLines ***
		//		public int DAQmxGetDONumLines(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_DO_Tristate ***
		//		public int DAQmxGetDOTristate(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOTristate(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOTristate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_LineStates_StartState ***
		//		// Uses value set DigitalLineState
		//		public int DAQmxGetDOLineStatesStartState(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOLineStatesStartState(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOLineStatesStartState(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_LineStates_PausedState ***
		//		// Uses value set DigitalLineState
		//		public int DAQmxGetDOLineStatesPausedState(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOLineStatesPausedState(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOLineStatesPausedState(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_LineStates_DoneState ***
		//		// Uses value set DigitalLineState
		//		public int DAQmxGetDOLineStatesDoneState(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOLineStatesDoneState(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOLineStatesDoneState(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_LogicFamily ***
		//		// Uses value set LogicFamily
		//		public int DAQmxGetDOLogicFamily(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOLogicFamily(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOLogicFamily(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_Overcurrent_Limit ***
		//		public int DAQmxGetDOOvercurrentLimit(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetDOOvercurrentLimit(int taskHandle, String channel, double data);
		//		public int DAQmxResetDOOvercurrentLimit(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_Overcurrent_AutoReenable ***
		//		public int DAQmxGetDOOvercurrentAutoReenable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOOvercurrentAutoReenable(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOOvercurrentAutoReenable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_Overcurrent_ReenablePeriod ***
		//		public int DAQmxGetDOOvercurrentReenablePeriod(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetDOOvercurrentReenablePeriod(int taskHandle, String channel, double data);
		//		public int DAQmxResetDOOvercurrentReenablePeriod(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_UseOnlyOnBrdMem ***
		//		public int DAQmxGetDOUseOnlyOnBrdMem(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOUseOnlyOnBrdMem(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOUseOnlyOnBrdMem(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_DataXferMech ***
		//		// Uses value set DataTransferMechanism
		//		public int DAQmxGetDODataXferMech(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDODataXferMech(int taskHandle, String channel, int data);
		//		public int DAQmxResetDODataXferMech(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_DataXferReqCond ***
		//		// Uses value set OutputDataTransferCondition
		//		public int DAQmxGetDODataXferReqCond(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDODataXferReqCond(int taskHandle, String channel, int data);
		//		public int DAQmxResetDODataXferReqCond(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_UsbXferReqSize ***
		//		public int DAQmxGetDOUsbXferReqSize(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOUsbXferReqSize(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOUsbXferReqSize(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_MemMapEnable ***
		//		public int DAQmxGetDOMemMapEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOMemMapEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOMemMapEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_DO_GenerateOn ***
		//		// Uses value set SampleClockActiveOrInactiveEdgeSelection
		//		public int DAQmxGetDOGenerateOn(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetDOGenerateOn(int taskHandle, String channel, int data);
		//		public int DAQmxResetDOGenerateOn(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Max ***
		//		public int DAQmxGetCIMax(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIMax(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIMax(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Min ***
		//		public int DAQmxGetCIMin(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIMin(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIMin(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CustomScaleName ***
		//		public int DAQmxGetCICustomScaleName(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCICustomScaleName(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCICustomScaleName(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_MeasType ***
		//		// Uses value set CIMeasurementType
		//		public int DAQmxGetCIMeasType(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CI_Freq_Units ***
		//		// Uses value set FrequencyUnits3
		//		public int DAQmxGetCIFreqUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIFreqUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIFreqUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_Term ***
		//		public int DAQmxGetCIFreqTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIFreqTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIFreqTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_StartingEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCIFreqStartingEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIFreqStartingEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIFreqStartingEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_MeasMeth ***
		//		// Uses value set CounterFrequencyMethod
		//		public int DAQmxGetCIFreqMeasMeth(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIFreqMeasMeth(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIFreqMeasMeth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_MeasTime ***
		//		public int DAQmxGetCIFreqMeasTime(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIFreqMeasTime(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIFreqMeasTime(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_Div ***
		//		public int DAQmxGetCIFreqDiv(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIFreqDiv(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIFreqDiv(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_DigFltr_Enable ***
		//		public int DAQmxGetCIFreqDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIFreqDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIFreqDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCIFreqDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIFreqDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIFreqDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCIFreqDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIFreqDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIFreqDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCIFreqDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIFreqDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIFreqDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Freq_DigSync_Enable ***
		//		public int DAQmxGetCIFreqDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIFreqDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIFreqDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_Units ***
		//		// Uses value set TimeUnits3
		//		public int DAQmxGetCIPeriodUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPeriodUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPeriodUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_Term ***
		//		public int DAQmxGetCIPeriodTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIPeriodTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIPeriodTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_StartingEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCIPeriodStartingEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPeriodStartingEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPeriodStartingEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_MeasMeth ***
		//		// Uses value set CounterFrequencyMethod
		//		public int DAQmxGetCIPeriodMeasMeth(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPeriodMeasMeth(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPeriodMeasMeth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_MeasTime ***
		//		public int DAQmxGetCIPeriodMeasTime(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIPeriodMeasTime(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIPeriodMeasTime(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_Div ***
		//		public int DAQmxGetCIPeriodDiv(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPeriodDiv(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPeriodDiv(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_DigFltr_Enable ***
		//		public int DAQmxGetCIPeriodDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPeriodDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPeriodDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCIPeriodDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIPeriodDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIPeriodDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCIPeriodDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIPeriodDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIPeriodDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCIPeriodDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIPeriodDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIPeriodDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Period_DigSync_Enable ***
		//		public int DAQmxGetCIPeriodDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPeriodDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPeriodDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_Term ***
		//		public int DAQmxGetCICountEdgesTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCICountEdgesTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCICountEdgesTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_Dir ***
		//		// Uses value set CountDirection1
		//		public int DAQmxGetCICountEdgesDir(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICountEdgesDir(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICountEdgesDir(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_DirTerm ***
		//		public int DAQmxGetCICountEdgesDirTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCICountEdgesDirTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCICountEdgesDirTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_CountDir_DigFltr_Enable ***
		//		public int DAQmxGetCICountEdgesCountDirDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICountEdgesCountDirDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICountEdgesCountDirDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_CountDir_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCICountEdgesCountDirDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCICountEdgesCountDirDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCICountEdgesCountDirDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_CountDir_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCICountEdgesCountDirDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCICountEdgesCountDirDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCICountEdgesCountDirDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_CountDir_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCICountEdgesCountDirDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCICountEdgesCountDirDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCICountEdgesCountDirDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_CountDir_DigSync_Enable ***
		//		public int DAQmxGetCICountEdgesCountDirDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICountEdgesCountDirDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICountEdgesCountDirDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_InitialCnt ***
		//		public int DAQmxGetCICountEdgesInitialCnt(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICountEdgesInitialCnt(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICountEdgesInitialCnt(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_ActiveEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCICountEdgesActiveEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICountEdgesActiveEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICountEdgesActiveEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_DigFltr_Enable ***
		//		public int DAQmxGetCICountEdgesDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICountEdgesDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICountEdgesDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCICountEdgesDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCICountEdgesDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCICountEdgesDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCICountEdgesDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCICountEdgesDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCICountEdgesDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCICountEdgesDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCICountEdgesDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCICountEdgesDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CountEdges_DigSync_Enable ***
		//		public int DAQmxGetCICountEdgesDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICountEdgesDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICountEdgesDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_AngEncoder_Units ***
		//		// Uses value set AngleUnits2
		//		public int DAQmxGetCIAngEncoderUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIAngEncoderUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIAngEncoderUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_AngEncoder_PulsesPerRev ***
		//		public int DAQmxGetCIAngEncoderPulsesPerRev(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIAngEncoderPulsesPerRev(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIAngEncoderPulsesPerRev(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_AngEncoder_InitialAngle ***
		//		public int DAQmxGetCIAngEncoderInitialAngle(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIAngEncoderInitialAngle(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIAngEncoderInitialAngle(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_LinEncoder_Units ***
		//		// Uses value set LengthUnits3
		//		public int DAQmxGetCILinEncoderUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCILinEncoderUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCILinEncoderUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_LinEncoder_DistPerPulse ***
		//		public int DAQmxGetCILinEncoderDistPerPulse(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCILinEncoderDistPerPulse(int taskHandle, String channel, double data);
		//		public int DAQmxResetCILinEncoderDistPerPulse(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_LinEncoder_InitialPos ***
		//		public int DAQmxGetCILinEncoderInitialPos(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCILinEncoderInitialPos(int taskHandle, String channel, double data);
		//		public int DAQmxResetCILinEncoderInitialPos(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_DecodingType ***
		//		// Uses value set EncoderType2
		//		public int DAQmxGetCIEncoderDecodingType(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderDecodingType(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderDecodingType(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_AInputTerm ***
		//		public int DAQmxGetCIEncoderAInputTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIEncoderAInputTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIEncoderAInputTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_AInput_DigFltr_Enable ***
		//		public int DAQmxGetCIEncoderAInputDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderAInputDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderAInputDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_AInput_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCIEncoderAInputDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIEncoderAInputDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIEncoderAInputDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_AInput_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCIEncoderAInputDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIEncoderAInputDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIEncoderAInputDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_AInput_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCIEncoderAInputDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIEncoderAInputDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIEncoderAInputDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_AInput_DigSync_Enable ***
		//		public int DAQmxGetCIEncoderAInputDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderAInputDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderAInputDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_BInputTerm ***
		//		public int DAQmxGetCIEncoderBInputTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIEncoderBInputTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIEncoderBInputTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_BInput_DigFltr_Enable ***
		//		public int DAQmxGetCIEncoderBInputDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderBInputDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderBInputDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_BInput_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCIEncoderBInputDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIEncoderBInputDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIEncoderBInputDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_BInput_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCIEncoderBInputDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIEncoderBInputDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIEncoderBInputDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_BInput_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCIEncoderBInputDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIEncoderBInputDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIEncoderBInputDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_BInput_DigSync_Enable ***
		//		public int DAQmxGetCIEncoderBInputDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderBInputDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderBInputDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZInputTerm ***
		//		public int DAQmxGetCIEncoderZInputTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIEncoderZInputTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIEncoderZInputTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZInput_DigFltr_Enable ***
		//		public int DAQmxGetCIEncoderZInputDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderZInputDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderZInputDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZInput_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCIEncoderZInputDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIEncoderZInputDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIEncoderZInputDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZInput_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCIEncoderZInputDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIEncoderZInputDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIEncoderZInputDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZInput_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCIEncoderZInputDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIEncoderZInputDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIEncoderZInputDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZInput_DigSync_Enable ***
		//		public int DAQmxGetCIEncoderZInputDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderZInputDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderZInputDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZIndexEnable ***
		//		public int DAQmxGetCIEncoderZIndexEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderZIndexEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderZIndexEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZIndexVal ***
		//		public int DAQmxGetCIEncoderZIndexVal(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIEncoderZIndexVal(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIEncoderZIndexVal(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Encoder_ZIndexPhase ***
		//		// Uses value set EncoderZIndexPhase1
		//		public int DAQmxGetCIEncoderZIndexPhase(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIEncoderZIndexPhase(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIEncoderZIndexPhase(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_Units ***
		//		// Uses value set TimeUnits3
		//		public int DAQmxGetCIPulseWidthUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPulseWidthUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPulseWidthUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_Term ***
		//		public int DAQmxGetCIPulseWidthTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIPulseWidthTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIPulseWidthTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_StartingEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCIPulseWidthStartingEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPulseWidthStartingEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPulseWidthStartingEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_DigFltr_Enable ***
		//		public int DAQmxGetCIPulseWidthDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPulseWidthDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPulseWidthDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCIPulseWidthDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIPulseWidthDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIPulseWidthDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCIPulseWidthDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIPulseWidthDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIPulseWidthDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCIPulseWidthDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCIPulseWidthDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCIPulseWidthDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_PulseWidth_DigSync_Enable ***
		//		public int DAQmxGetCIPulseWidthDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPulseWidthDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPulseWidthDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_Units ***
		//		// Uses value set TimeUnits3
		//		public int DAQmxGetCITwoEdgeSepUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITwoEdgeSepUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITwoEdgeSepUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_FirstTerm ***
		//		public int DAQmxGetCITwoEdgeSepFirstTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCITwoEdgeSepFirstTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCITwoEdgeSepFirstTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_FirstEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCITwoEdgeSepFirstEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITwoEdgeSepFirstEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITwoEdgeSepFirstEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_First_DigFltr_Enable ***
		//		public int DAQmxGetCITwoEdgeSepFirstDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITwoEdgeSepFirstDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITwoEdgeSepFirstDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_First_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCITwoEdgeSepFirstDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCITwoEdgeSepFirstDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCITwoEdgeSepFirstDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_First_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCITwoEdgeSepFirstDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCITwoEdgeSepFirstDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCITwoEdgeSepFirstDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_First_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCITwoEdgeSepFirstDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCITwoEdgeSepFirstDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCITwoEdgeSepFirstDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_First_DigSync_Enable ***
		//		public int DAQmxGetCITwoEdgeSepFirstDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITwoEdgeSepFirstDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITwoEdgeSepFirstDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_SecondTerm ***
		//		public int DAQmxGetCITwoEdgeSepSecondTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCITwoEdgeSepSecondTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCITwoEdgeSepSecondTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_SecondEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCITwoEdgeSepSecondEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITwoEdgeSepSecondEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITwoEdgeSepSecondEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_Second_DigFltr_Enable ***
		//		public int DAQmxGetCITwoEdgeSepSecondDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITwoEdgeSepSecondDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITwoEdgeSepSecondDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_Second_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCITwoEdgeSepSecondDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCITwoEdgeSepSecondDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCITwoEdgeSepSecondDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_Second_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCITwoEdgeSepSecondDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCITwoEdgeSepSecondDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCITwoEdgeSepSecondDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_Second_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCITwoEdgeSepSecondDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCITwoEdgeSepSecondDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCITwoEdgeSepSecondDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_TwoEdgeSep_Second_DigSync_Enable ***
		//		public int DAQmxGetCITwoEdgeSepSecondDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITwoEdgeSepSecondDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITwoEdgeSepSecondDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_Units ***
		//		// Uses value set TimeUnits3
		//		public int DAQmxGetCISemiPeriodUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCISemiPeriodUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCISemiPeriodUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_Term ***
		//		public int DAQmxGetCISemiPeriodTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCISemiPeriodTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCISemiPeriodTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_StartingEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCISemiPeriodStartingEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCISemiPeriodStartingEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCISemiPeriodStartingEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_DigFltr_Enable ***
		//		public int DAQmxGetCISemiPeriodDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCISemiPeriodDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCISemiPeriodDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCISemiPeriodDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCISemiPeriodDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCISemiPeriodDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCISemiPeriodDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCISemiPeriodDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCISemiPeriodDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCISemiPeriodDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCISemiPeriodDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCISemiPeriodDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_SemiPeriod_DigSync_Enable ***
		//		public int DAQmxGetCISemiPeriodDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCISemiPeriodDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCISemiPeriodDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Timestamp_Units ***
		//		// Uses value set TimeUnits
		//		public int DAQmxGetCITimestampUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITimestampUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITimestampUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Timestamp_InitialSeconds ***
		//		public int DAQmxGetCITimestampInitialSeconds(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCITimestampInitialSeconds(int taskHandle, String channel, int data);
		//		public int DAQmxResetCITimestampInitialSeconds(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_GPS_SyncMethod ***
		//		// Uses value set GpsSignalType1
		//		public int DAQmxGetCIGPSSyncMethod(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIGPSSyncMethod(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIGPSSyncMethod(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_GPS_SyncSrc ***
		//		public int DAQmxGetCIGPSSyncSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCIGPSSyncSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCIGPSSyncSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebaseSrc ***
		//		public int DAQmxGetCICtrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCICtrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCICtrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebaseRate ***
		//		public int DAQmxGetCICtrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCICtrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCICtrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebaseActiveEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCICtrTimebaseActiveEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICtrTimebaseActiveEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICtrTimebaseActiveEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebase_DigFltr_Enable ***
		//		public int DAQmxGetCICtrTimebaseDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICtrTimebaseDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICtrTimebaseDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebase_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCICtrTimebaseDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCICtrTimebaseDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCICtrTimebaseDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebase_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCICtrTimebaseDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCICtrTimebaseDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCICtrTimebaseDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebase_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCICtrTimebaseDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCICtrTimebaseDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCICtrTimebaseDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebase_DigSync_Enable ***
		//		public int DAQmxGetCICtrTimebaseDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICtrTimebaseDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICtrTimebaseDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Count ***
		//		public int DAQmxGetCICount(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CI_OutputState ***
		//		// Uses value set Level1
		//		public int DAQmxGetCIOutputState(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CI_TCReached ***
		//		public int DAQmxGetCITCReached(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CI_CtrTimebaseMasterTimebaseDiv ***
		//		public int DAQmxGetCICtrTimebaseMasterTimebaseDiv(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCICtrTimebaseMasterTimebaseDiv(int taskHandle, String channel, int data);
		//		public int DAQmxResetCICtrTimebaseMasterTimebaseDiv(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_DataXferMech ***
		//		// Uses value set DataTransferMechanism
		//		public int DAQmxGetCIDataXferMech(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIDataXferMech(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIDataXferMech(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_UsbXferReqSize ***
		//		public int DAQmxGetCIUsbXferReqSize(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIUsbXferReqSize(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIUsbXferReqSize(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_NumPossiblyInvalidSamps ***
		//		public int DAQmxGetCINumPossiblyInvalidSamps(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CI_DupCountPrevent ***
		//		public int DAQmxGetCIDupCountPrevent(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIDupCountPrevent(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIDupCountPrevent(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CI_Prescaler ***
		//		public int DAQmxGetCIPrescaler(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCIPrescaler(int taskHandle, String channel, int data);
		//		public int DAQmxResetCIPrescaler(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_OutputType ***
		//		// Uses value set COOutputType
		//		public int DAQmxGetCOOutputType(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_IdleState ***
		//		// Uses value set Level1
		//		public int DAQmxGetCOPulseIdleState(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOPulseIdleState(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOPulseIdleState(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_Term ***
		//		public int DAQmxGetCOPulseTerm(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCOPulseTerm(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCOPulseTerm(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_Time_Units ***
		//		// Uses value set TimeUnits2
		//		public int DAQmxGetCOPulseTimeUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOPulseTimeUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOPulseTimeUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_HighTime ***
		//		public int DAQmxGetCOPulseHighTime(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOPulseHighTime(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOPulseHighTime(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_LowTime ***
		//		public int DAQmxGetCOPulseLowTime(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOPulseLowTime(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOPulseLowTime(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_Time_InitialDelay ***
		//		public int DAQmxGetCOPulseTimeInitialDelay(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOPulseTimeInitialDelay(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOPulseTimeInitialDelay(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_DutyCyc ***
		//		public int DAQmxGetCOPulseDutyCyc(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOPulseDutyCyc(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOPulseDutyCyc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_Freq_Units ***
		//		// Uses value set FrequencyUnits2
		//		public int DAQmxGetCOPulseFreqUnits(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOPulseFreqUnits(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOPulseFreqUnits(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_Freq ***
		//		public int DAQmxGetCOPulseFreq(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOPulseFreq(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOPulseFreq(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_Freq_InitialDelay ***
		//		public int DAQmxGetCOPulseFreqInitialDelay(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOPulseFreqInitialDelay(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOPulseFreqInitialDelay(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_HighTicks ***
		//		public int DAQmxGetCOPulseHighTicks(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOPulseHighTicks(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOPulseHighTicks(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_LowTicks ***
		//		public int DAQmxGetCOPulseLowTicks(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOPulseLowTicks(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOPulseLowTicks(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Pulse_Ticks_InitialDelay ***
		//		public int DAQmxGetCOPulseTicksInitialDelay(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOPulseTicksInitialDelay(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOPulseTicksInitialDelay(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebaseSrc ***
		//		public int DAQmxGetCOCtrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCOCtrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCOCtrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebaseRate ***
		//		public int DAQmxGetCOCtrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOCtrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOCtrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebaseActiveEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetCOCtrTimebaseActiveEdge(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOCtrTimebaseActiveEdge(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOCtrTimebaseActiveEdge(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebase_DigFltr_Enable ***
		//		public int DAQmxGetCOCtrTimebaseDigFltrEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOCtrTimebaseDigFltrEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOCtrTimebaseDigFltrEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebase_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetCOCtrTimebaseDigFltrMinPulseWidth(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOCtrTimebaseDigFltrMinPulseWidth(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOCtrTimebaseDigFltrMinPulseWidth(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebase_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetCOCtrTimebaseDigFltrTimebaseSrc(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetCOCtrTimebaseDigFltrTimebaseSrc(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetCOCtrTimebaseDigFltrTimebaseSrc(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebase_DigFltr_TimebaseRate ***
		//		public int DAQmxGetCOCtrTimebaseDigFltrTimebaseRate(int taskHandle, String channel, DoubleByReference data);
		//		public int DAQmxSetCOCtrTimebaseDigFltrTimebaseRate(int taskHandle, String channel, double data);
		//		public int DAQmxResetCOCtrTimebaseDigFltrTimebaseRate(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebase_DigSync_Enable ***
		//		public int DAQmxGetCOCtrTimebaseDigSyncEnable(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOCtrTimebaseDigSyncEnable(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOCtrTimebaseDigSyncEnable(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Count ***
		//		public int DAQmxGetCOCount(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CO_OutputState ***
		//		// Uses value set Level1
		//		public int DAQmxGetCOOutputState(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CO_AutoIncrCnt ***
		//		public int DAQmxGetCOAutoIncrCnt(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOAutoIncrCnt(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOAutoIncrCnt(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_CtrTimebaseMasterTimebaseDiv ***
		//		public int DAQmxGetCOCtrTimebaseMasterTimebaseDiv(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOCtrTimebaseMasterTimebaseDiv(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOCtrTimebaseMasterTimebaseDiv(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_PulseDone ***
		//		public int DAQmxGetCOPulseDone(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_CO_ConstrainedGenMode ***
		//		// Uses value set ConstrainedGenMode
		//		public int DAQmxGetCOConstrainedGenMode(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOConstrainedGenMode(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOConstrainedGenMode(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_Prescaler ***
		//		public int DAQmxGetCOPrescaler(int taskHandle, String channel, IntByReference data);
		//		public int DAQmxSetCOPrescaler(int taskHandle, String channel, int data);
		//		public int DAQmxResetCOPrescaler(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_CO_RdyForNewVal ***
		//		public int DAQmxGetCORdyForNewVal(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_ChanType ***
		//		// Uses value set ChannelType
		//		public int DAQmxGetChanType(int taskHandle, String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChanName ***
		//		public int DAQmxGetPhysicalChanName(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetPhysicalChanName(int taskHandle, String channel, final byte[] data);
		//		//*** Set/Get functions for DAQmx_ChanDescr ***
		//		public int DAQmxGetChanDescr(int taskHandle, String channel, byte[] data, int bufferSize);
		//		public int DAQmxSetChanDescr(int taskHandle, String channel, final byte[] data);
		//		public int DAQmxResetChanDescr(int taskHandle, String channel);
		//		//*** Set/Get functions for DAQmx_ChanIsGlobal ***
		//		public int DAQmxGetChanIsGlobal(int taskHandle, String channel, IntByReference data);
		//
		//		//********** Export Signal **********
		//		//*** Set/Get functions for DAQmx_Exported_AIConvClk_OutputTerm ***
		//		public int DAQmxGetExportedAIConvClkOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedAIConvClkOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedAIConvClkOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AIConvClk_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedAIConvClkPulsePolarity(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Exported_10MHzRefClk_OutputTerm ***
		//		public int DAQmxGetExported10MHzRefClkOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExported10MHzRefClkOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExported10MHzRefClkOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_20MHzTimebase_OutputTerm ***
		//		public int DAQmxGetExported20MHzTimebaseOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExported20MHzTimebaseOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExported20MHzTimebaseOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_SampClk_OutputBehavior ***
		//		// Uses value set ExportActions3
		//		public int DAQmxGetExportedSampClkOutputBehavior(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedSampClkOutputBehavior(int taskHandle, int data);
		//		public int DAQmxResetExportedSampClkOutputBehavior(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_SampClk_OutputTerm ***
		//		public int DAQmxGetExportedSampClkOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedSampClkOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedSampClkOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_SampClk_DelayOffset ***
		//		public int DAQmxGetExportedSampClkDelayOffset(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetExportedSampClkDelayOffset(int taskHandle, double data);
		//		public int DAQmxResetExportedSampClkDelayOffset(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_SampClk_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedSampClkPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedSampClkPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedSampClkPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_SampClkTimebase_OutputTerm ***
		//		public int DAQmxGetExportedSampClkTimebaseOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedSampClkTimebaseOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedSampClkTimebaseOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_DividedSampClkTimebase_OutputTerm ***
		//		public int DAQmxGetExportedDividedSampClkTimebaseOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedDividedSampClkTimebaseOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedDividedSampClkTimebaseOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AdvTrig_OutputTerm ***
		//		public int DAQmxGetExportedAdvTrigOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedAdvTrigOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedAdvTrigOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AdvTrig_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedAdvTrigPulsePolarity(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Exported_AdvTrig_Pulse_WidthUnits ***
		//		// Uses value set DigitalWidthUnits3
		//		public int DAQmxGetExportedAdvTrigPulseWidthUnits(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedAdvTrigPulseWidthUnits(int taskHandle, int data);
		//		public int DAQmxResetExportedAdvTrigPulseWidthUnits(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AdvTrig_Pulse_Width ***
		//		public int DAQmxGetExportedAdvTrigPulseWidth(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetExportedAdvTrigPulseWidth(int taskHandle, double data);
		//		public int DAQmxResetExportedAdvTrigPulseWidth(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_PauseTrig_OutputTerm ***
		//		public int DAQmxGetExportedPauseTrigOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedPauseTrigOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedPauseTrigOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_PauseTrig_Lvl_ActiveLvl ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedPauseTrigLvlActiveLvl(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedPauseTrigLvlActiveLvl(int taskHandle, int data);
		//		public int DAQmxResetExportedPauseTrigLvlActiveLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RefTrig_OutputTerm ***
		//		public int DAQmxGetExportedRefTrigOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedRefTrigOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedRefTrigOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RefTrig_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedRefTrigPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedRefTrigPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedRefTrigPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_StartTrig_OutputTerm ***
		//		public int DAQmxGetExportedStartTrigOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedStartTrigOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedStartTrigOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_StartTrig_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedStartTrigPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedStartTrigPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedStartTrigPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AdvCmpltEvent_OutputTerm ***
		//		public int DAQmxGetExportedAdvCmpltEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedAdvCmpltEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedAdvCmpltEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AdvCmpltEvent_Delay ***
		//		public int DAQmxGetExportedAdvCmpltEventDelay(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetExportedAdvCmpltEventDelay(int taskHandle, double data);
		//		public int DAQmxResetExportedAdvCmpltEventDelay(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AdvCmpltEvent_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedAdvCmpltEventPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedAdvCmpltEventPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedAdvCmpltEventPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AdvCmpltEvent_Pulse_Width ***
		//		public int DAQmxGetExportedAdvCmpltEventPulseWidth(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetExportedAdvCmpltEventPulseWidth(int taskHandle, double data);
		//		public int DAQmxResetExportedAdvCmpltEventPulseWidth(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AIHoldCmpltEvent_OutputTerm ***
		//		public int DAQmxGetExportedAIHoldCmpltEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedAIHoldCmpltEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedAIHoldCmpltEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_AIHoldCmpltEvent_PulsePolarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedAIHoldCmpltEventPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedAIHoldCmpltEventPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedAIHoldCmpltEventPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_ChangeDetectEvent_OutputTerm ***
		//		public int DAQmxGetExportedChangeDetectEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedChangeDetectEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedChangeDetectEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_ChangeDetectEvent_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedChangeDetectEventPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedChangeDetectEventPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedChangeDetectEventPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_CtrOutEvent_OutputTerm ***
		//		public int DAQmxGetExportedCtrOutEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedCtrOutEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedCtrOutEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_CtrOutEvent_OutputBehavior ***
		//		// Uses value set ExportActions2
		//		public int DAQmxGetExportedCtrOutEventOutputBehavior(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedCtrOutEventOutputBehavior(int taskHandle, int data);
		//		public int DAQmxResetExportedCtrOutEventOutputBehavior(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_CtrOutEvent_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedCtrOutEventPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedCtrOutEventPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedCtrOutEventPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_CtrOutEvent_Toggle_IdleState ***
		//		// Uses value set Level1
		//		public int DAQmxGetExportedCtrOutEventToggleIdleState(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedCtrOutEventToggleIdleState(int taskHandle, int data);
		//		public int DAQmxResetExportedCtrOutEventToggleIdleState(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_OutputTerm ***
		//		public int DAQmxGetExportedHshkEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedHshkEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedHshkEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_OutputBehavior ***
		//		// Uses value set ExportActions5
		//		public int DAQmxGetExportedHshkEventOutputBehavior(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedHshkEventOutputBehavior(int taskHandle, int data);
		//		public int DAQmxResetExportedHshkEventOutputBehavior(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_Delay ***
		//		public int DAQmxGetExportedHshkEventDelay(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetExportedHshkEventDelay(int taskHandle, double data);
		//		public int DAQmxResetExportedHshkEventDelay(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_Interlocked_AssertedLvl ***
		//		// Uses value set Level1
		//		public int DAQmxGetExportedHshkEventInterlockedAssertedLvl(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedHshkEventInterlockedAssertedLvl(int taskHandle, int data);
		//		public int DAQmxResetExportedHshkEventInterlockedAssertedLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_Interlocked_AssertOnStart ***
		//		public int DAQmxGetExportedHshkEventInterlockedAssertOnStart(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedHshkEventInterlockedAssertOnStart(int taskHandle, int data);
		//		public int DAQmxResetExportedHshkEventInterlockedAssertOnStart(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_Interlocked_DeassertDelay ***
		//		public int DAQmxGetExportedHshkEventInterlockedDeassertDelay(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetExportedHshkEventInterlockedDeassertDelay(int taskHandle, double data);
		//		public int DAQmxResetExportedHshkEventInterlockedDeassertDelay(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_Pulse_Polarity ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedHshkEventPulsePolarity(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedHshkEventPulsePolarity(int taskHandle, int data);
		//		public int DAQmxResetExportedHshkEventPulsePolarity(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_HshkEvent_Pulse_Width ***
		//		public int DAQmxGetExportedHshkEventPulseWidth(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetExportedHshkEventPulseWidth(int taskHandle, double data);
		//		public int DAQmxResetExportedHshkEventPulseWidth(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RdyForXferEvent_OutputTerm ***
		//		public int DAQmxGetExportedRdyForXferEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedRdyForXferEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedRdyForXferEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RdyForXferEvent_Lvl_ActiveLvl ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedRdyForXferEventLvlActiveLvl(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedRdyForXferEventLvlActiveLvl(int taskHandle, int data);
		//		public int DAQmxResetExportedRdyForXferEventLvlActiveLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RdyForXferEvent_DeassertCond ***
		//		// Uses value set DeassertCondition
		//		public int DAQmxGetExportedRdyForXferEventDeassertCond(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedRdyForXferEventDeassertCond(int taskHandle, int data);
		//		public int DAQmxResetExportedRdyForXferEventDeassertCond(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RdyForXferEvent_DeassertCondCustomThreshold ***
		//		public int DAQmxGetExportedRdyForXferEventDeassertCondCustomThreshold(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedRdyForXferEventDeassertCondCustomThreshold(int taskHandle, int data);
		//		public int DAQmxResetExportedRdyForXferEventDeassertCondCustomThreshold(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_DataActiveEvent_OutputTerm ***
		//		public int DAQmxGetExportedDataActiveEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedDataActiveEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedDataActiveEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_DataActiveEvent_Lvl_ActiveLvl ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedDataActiveEventLvlActiveLvl(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedDataActiveEventLvlActiveLvl(int taskHandle, int data);
		//		public int DAQmxResetExportedDataActiveEventLvlActiveLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RdyForStartEvent_OutputTerm ***
		//		public int DAQmxGetExportedRdyForStartEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedRdyForStartEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedRdyForStartEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_RdyForStartEvent_Lvl_ActiveLvl ***
		//		// Uses value set Polarity2
		//		public int DAQmxGetExportedRdyForStartEventLvlActiveLvl(int taskHandle, IntByReference data);
		//		public int DAQmxSetExportedRdyForStartEventLvlActiveLvl(int taskHandle, int data);
		//		public int DAQmxResetExportedRdyForStartEventLvlActiveLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_SyncPulseEvent_OutputTerm ***
		//		public int DAQmxGetExportedSyncPulseEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedSyncPulseEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedSyncPulseEventOutputTerm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Exported_WatchdogExpiredEvent_OutputTerm ***
		//		public int DAQmxGetExportedWatchdogExpiredEventOutputTerm(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetExportedWatchdogExpiredEventOutputTerm(int taskHandle, final byte[] data);
		//		public int DAQmxResetExportedWatchdogExpiredEventOutputTerm(int taskHandle);
		//
		//********** Device **********
		//*** Set/Get functions for DAQmx_Dev_IsSimulated ***
		public int DAQmxGetDevIsSimulated(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_ProductCategory ***
		// Uses value set ProductCategory
		public int DAQmxGetDevProductCategory(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_ProductType ***
		public int DAQmxGetDevProductType(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_ProductNum ***
		public int DAQmxGetDevProductNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_SerialNum ***
		public int DAQmxGetDevSerialNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Carrier_SerialNum ***
		public int DAQmxGetCarrierSerialNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_Chassis_ModuleDevNames ***
		public int DAQmxGetDevChassisModuleDevNames(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_AnlgTrigSupported ***
		public int DAQmxGetDevAnlgTrigSupported(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_DigTrigSupported ***
		public int DAQmxGetDevDigTrigSupported(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_AI_PhysicalChans ***
		public int DAQmxGetDevAIPhysicalChans(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_AI_MaxSingleChanRate ***
		public int DAQmxGetDevAIMaxSingleChanRate(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_AI_MaxMultiChanRate ***
		public int DAQmxGetDevAIMaxMultiChanRate(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_AI_MinRate ***
		public int DAQmxGetDevAIMinRate(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_AI_SimultaneousSamplingSupported ***
		public int DAQmxGetDevAISimultaneousSamplingSupported(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_AI_TrigUsage ***
		// Uses bits from enum TriggerUsageTypeBits
		public int DAQmxGetDevAITrigUsage(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_AI_VoltageRngs ***
		public int DAQmxGetDevAIVoltageRngs(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_VoltageIntExcitDiscreteVals ***
		public int DAQmxGetDevAIVoltageIntExcitDiscreteVals(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_VoltageIntExcitRangeVals ***
		public int DAQmxGetDevAIVoltageIntExcitRangeVals(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_CurrentRngs ***
		public int DAQmxGetDevAICurrentRngs(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_CurrentIntExcitDiscreteVals ***
		public int DAQmxGetDevAICurrentIntExcitDiscreteVals(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_FreqRngs ***
		public int DAQmxGetDevAIFreqRngs(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_Gains ***
		public int DAQmxGetDevAIGains(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_Couplings ***
		// Uses bits from enum CouplingTypeBits
		public int DAQmxGetDevAICouplings(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_AI_LowpassCutoffFreqDiscreteVals ***
		public int DAQmxGetDevAILowpassCutoffFreqDiscreteVals(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AI_LowpassCutoffFreqRangeVals ***
		public int DAQmxGetDevAILowpassCutoffFreqRangeVals(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AO_PhysicalChans ***
		public int DAQmxGetDevAOPhysicalChans(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_AO_SampClkSupported ***
		public int DAQmxGetDevAOSampClkSupported(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_AO_MaxRate ***
		public int DAQmxGetDevAOMaxRate(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_AO_MinRate ***
		public int DAQmxGetDevAOMinRate(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_AO_TrigUsage ***
		// Uses bits from enum TriggerUsageTypeBits
		public int DAQmxGetDevAOTrigUsage(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_AO_VoltageRngs ***
		public int DAQmxGetDevAOVoltageRngs(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AO_CurrentRngs ***
		public int DAQmxGetDevAOCurrentRngs(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_AO_Gains ***
		public int DAQmxGetDevAOGains(String device, double[] data, int arraySizeInSamples);
		//*** Set/Get functions for DAQmx_Dev_DI_Lines ***
		public int DAQmxGetDevDILines(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_DI_Ports ***
		public int DAQmxGetDevDIPorts(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_DI_MaxRate ***
		public int DAQmxGetDevDIMaxRate(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_DI_TrigUsage ***
		// Uses bits from enum TriggerUsageTypeBits
		public int DAQmxGetDevDITrigUsage(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_DO_Lines ***
		public int DAQmxGetDevDOLines(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_DO_Ports ***
		public int DAQmxGetDevDOPorts(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_DO_MaxRate ***
		public int DAQmxGetDevDOMaxRate(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_DO_TrigUsage ***
		// Uses bits from enum TriggerUsageTypeBits
		public int DAQmxGetDevDOTrigUsage(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_CI_PhysicalChans ***
		public int DAQmxGetDevCIPhysicalChans(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_CI_TrigUsage ***
		// Uses bits from enum TriggerUsageTypeBits
		public int DAQmxGetDevCITrigUsage(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_CI_SampClkSupported ***
		public int DAQmxGetDevCISampClkSupported(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_CI_MaxSize ***
		public int DAQmxGetDevCIMaxSize(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_CI_MaxTimebase ***
		public int DAQmxGetDevCIMaxTimebase(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_CO_PhysicalChans ***
		public int DAQmxGetDevCOPhysicalChans(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_CO_TrigUsage ***
		// Uses bits from enum TriggerUsageTypeBits
		public int DAQmxGetDevCOTrigUsage(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_CO_MaxSize ***
		public int DAQmxGetDevCOMaxSize(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_CO_MaxTimebase ***
		public int DAQmxGetDevCOMaxTimebase(String device, DoubleByReference data);
		//*** Set/Get functions for DAQmx_Dev_NumDMAChans ***
		public int DAQmxGetDevNumDMAChans(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_BusType ***
		// Uses value set BusType
		public int DAQmxGetDevBusType(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_PCI_BusNum ***
		public int DAQmxGetDevPCIBusNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_PCI_DevNum ***
		public int DAQmxGetDevPCIDevNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_PXI_ChassisNum ***
		public int DAQmxGetDevPXIChassisNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_PXI_SlotNum ***
		public int DAQmxGetDevPXISlotNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_CompactDAQ_ChassisDevName ***
		public int DAQmxGetDevCompactDAQChassisDevName(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_CompactDAQ_SlotNum ***
		public int DAQmxGetDevCompactDAQSlotNum(String device, IntByReference data);
		//*** Set/Get functions for DAQmx_Dev_TCPIP_Hostname ***
		public int DAQmxGetDevTCPIPHostname(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_TCPIP_EthernetIP ***
		public int DAQmxGetDevTCPIPEthernetIP(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_TCPIP_WirelessIP ***
		public int DAQmxGetDevTCPIPWirelessIP(String device, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Dev_Terminals ***
		public int DAQmxGetDevTerminals(String device, byte[] data, int bufferSize);

		//		//********** Read **********
		//		//*** Set/Get functions for DAQmx_Read_RelativeTo ***
		//		// Uses value set ReadRelativeTo
		//		public int DAQmxGetReadRelativeTo(int taskHandle, IntByReference data);
		//		public int DAQmxSetReadRelativeTo(int taskHandle, int data);
		//		public int DAQmxResetReadRelativeTo(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Read_Offset ***
		//		public int DAQmxGetReadOffset(int taskHandle, IntByReference data);
		//		public int DAQmxSetReadOffset(int taskHandle, int data);
		//		public int DAQmxResetReadOffset(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Read_ChannelsToRead ***
		//		public int DAQmxGetReadChannelsToRead(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetReadChannelsToRead(int taskHandle, final byte[] data);
		//		public int DAQmxResetReadChannelsToRead(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Read_ReadAllAvailSamp ***
		//		public int DAQmxGetReadReadAllAvailSamp(int taskHandle, IntByReference data);
		//		public int DAQmxSetReadReadAllAvailSamp(int taskHandle, int data);
		//		public int DAQmxResetReadReadAllAvailSamp(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Read_AutoStart ***
		//		public int DAQmxGetReadAutoStart(int taskHandle, IntByReference data);
		//		public int DAQmxSetReadAutoStart(int taskHandle, int data);
		//		public int DAQmxResetReadAutoStart(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Read_OverWrite ***
		//		// Uses value set OverwriteMode1
		//		public int DAQmxGetReadOverWrite(int taskHandle, IntByReference data);
		//		public int DAQmxSetReadOverWrite(int taskHandle, int data);
		//		public int DAQmxResetReadOverWrite(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Read_CurrReadPos ***
		//		public int DAQmxGetReadCurrReadPos(int taskHandle, LongByReference data);
		//		//*** Set/Get functions for DAQmx_Read_AvailSampPerChan ***
		//		public int DAQmxGetReadAvailSampPerChan(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_TotalSampPerChanAcquired ***
		//		public int DAQmxGetReadTotalSampPerChanAcquired(int taskHandle, LongByReference data);
		//		//*** Set/Get functions for DAQmx_Read_CommonModeRangeErrorChansExist ***
		//		public int DAQmxGetReadCommonModeRangeErrorChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_CommonModeRangeErrorChans ***
		//		public int DAQmxGetReadCommonModeRangeErrorChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Read_OvercurrentChansExist ***
		//		public int DAQmxGetReadOvercurrentChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_OvercurrentChans ***
		//		public int DAQmxGetReadOvercurrentChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Read_OpenCurrentLoopChansExist ***
		//		public int DAQmxGetReadOpenCurrentLoopChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_OpenCurrentLoopChans ***
		//		public int DAQmxGetReadOpenCurrentLoopChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Read_OpenThrmcplChansExist ***
		//		public int DAQmxGetReadOpenThrmcplChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_OpenThrmcplChans ***
		//		public int DAQmxGetReadOpenThrmcplChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Read_OverloadedChansExist ***
		//		public int DAQmxGetReadOverloadedChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_OverloadedChans ***
		//		public int DAQmxGetReadOverloadedChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Read_ChangeDetect_HasOverflowed ***
		//		public int DAQmxGetReadChangeDetectHasOverflowed(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_RawDataWidth ***
		//		public int DAQmxGetReadRawDataWidth(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_NumChans ***
		//		public int DAQmxGetReadNumChans(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_DigitalLines_BytesPerChan ***
		//		public int DAQmxGetReadDigitalLinesBytesPerChan(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Read_WaitMode ***
		//		// Uses value set WaitMode
		//		public int DAQmxGetReadWaitMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetReadWaitMode(int taskHandle, int data);
		//		public int DAQmxResetReadWaitMode(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Read_SleepTime ***
		//		public int DAQmxGetReadSleepTime(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetReadSleepTime(int taskHandle, double data);
		//		public int DAQmxResetReadSleepTime(int taskHandle);
		//
		//		//********** Real-Time **********
		//		//*** Set/Get functions for DAQmx_RealTime_ConvLateErrorsToWarnings ***
		//		public int DAQmxGetRealTimeConvLateErrorsToWarnings(int taskHandle, IntByReference data);
		//		public int DAQmxSetRealTimeConvLateErrorsToWarnings(int taskHandle, int data);
		//		public int DAQmxResetRealTimeConvLateErrorsToWarnings(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RealTime_NumOfWarmupIters ***
		//		public int DAQmxGetRealTimeNumOfWarmupIters(int taskHandle, IntByReference data);
		//		public int DAQmxSetRealTimeNumOfWarmupIters(int taskHandle, int data);
		//		public int DAQmxResetRealTimeNumOfWarmupIters(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RealTime_WaitForNextSampClkWaitMode ***
		//		// Uses value set WaitMode3
		//		public int DAQmxGetRealTimeWaitForNextSampClkWaitMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetRealTimeWaitForNextSampClkWaitMode(int taskHandle, int data);
		//		public int DAQmxResetRealTimeWaitForNextSampClkWaitMode(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RealTime_ReportMissedSamp ***
		//		public int DAQmxGetRealTimeReportMissedSamp(int taskHandle, IntByReference data);
		//		public int DAQmxSetRealTimeReportMissedSamp(int taskHandle, int data);
		//		public int DAQmxResetRealTimeReportMissedSamp(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RealTime_WriteRecoveryMode ***
		//		// Uses value set WaitMode4
		//		public int DAQmxGetRealTimeWriteRecoveryMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetRealTimeWriteRecoveryMode(int taskHandle, int data);
		//		public int DAQmxResetRealTimeWriteRecoveryMode(int taskHandle);
		//
		//		//********** Switch Channel **********
		//		//*** Set/Get functions for DAQmx_SwitchChan_Usage ***
		//		// Uses value set SwitchUsageTypes
		//		public int DAQmxGetSwitchChanUsage(String switchChannelName, IntByReference data);
		//		public int DAQmxSetSwitchChanUsage(String switchChannelName, int data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxACCarryCurrent ***
		//		public int DAQmxGetSwitchChanMaxACCarryCurrent(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxACSwitchCurrent ***
		//		public int DAQmxGetSwitchChanMaxACSwitchCurrent(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxACCarryPwr ***
		//		public int DAQmxGetSwitchChanMaxACCarryPwr(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxACSwitchPwr ***
		//		public int DAQmxGetSwitchChanMaxACSwitchPwr(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxDCCarryCurrent ***
		//		public int DAQmxGetSwitchChanMaxDCCarryCurrent(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxDCSwitchCurrent ***
		//		public int DAQmxGetSwitchChanMaxDCSwitchCurrent(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxDCCarryPwr ***
		//		public int DAQmxGetSwitchChanMaxDCCarryPwr(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxDCSwitchPwr ***
		//		public int DAQmxGetSwitchChanMaxDCSwitchPwr(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxACVoltage ***
		//		public int DAQmxGetSwitchChanMaxACVoltage(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_MaxDCVoltage ***
		//		public int DAQmxGetSwitchChanMaxDCVoltage(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_WireMode ***
		//		public int DAQmxGetSwitchChanWireMode(String switchChannelName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_Bandwidth ***
		//		public int DAQmxGetSwitchChanBandwidth(String switchChannelName, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchChan_Impedance ***
		//		public int DAQmxGetSwitchChanImpedance(String switchChannelName, DoubleByReference data);
		//
		//		//********** Switch Device **********
		//		//*** Set/Get functions for DAQmx_SwitchDev_SettlingTime ***
		//		public int DAQmxGetSwitchDevSettlingTime(String deviceName, DoubleByReference data);
		//		public int DAQmxSetSwitchDevSettlingTime(String deviceName, double data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_AutoConnAnlgBus ***
		//		public int DAQmxGetSwitchDevAutoConnAnlgBus(String deviceName, IntByReference data);
		//		public int DAQmxSetSwitchDevAutoConnAnlgBus(String deviceName, int data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_PwrDownLatchRelaysAfterSettling ***
		//		public int DAQmxGetSwitchDevPwrDownLatchRelaysAfterSettling(String deviceName, IntByReference data);
		//		public int DAQmxSetSwitchDevPwrDownLatchRelaysAfterSettling(String deviceName, int data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_Settled ***
		//		public int DAQmxGetSwitchDevSettled(String deviceName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_RelayList ***
		//		public int DAQmxGetSwitchDevRelayList(String deviceName, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_SwitchDev_NumRelays ***
		//		public int DAQmxGetSwitchDevNumRelays(String deviceName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_SwitchChanList ***
		//		public int DAQmxGetSwitchDevSwitchChanList(String deviceName, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_SwitchDev_NumSwitchChans ***
		//		public int DAQmxGetSwitchDevNumSwitchChans(String deviceName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_NumRows ***
		//		public int DAQmxGetSwitchDevNumRows(String deviceName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_NumColumns ***
		//		public int DAQmxGetSwitchDevNumColumns(String deviceName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_SwitchDev_Topology ***
		//		public int DAQmxGetSwitchDevTopology(String deviceName, byte[] data, int bufferSize);
		//
		//		//********** Switch Scan **********
		//		//*** Set/Get functions for DAQmx_SwitchScan_BreakMode ***
		//		// Uses value set BreakMode
		//		public int DAQmxGetSwitchScanBreakMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetSwitchScanBreakMode(int taskHandle, int data);
		//		public int DAQmxResetSwitchScanBreakMode(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SwitchScan_RepeatMode ***
		//		// Uses value set SwitchScanRepeatMode
		//		public int DAQmxGetSwitchScanRepeatMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetSwitchScanRepeatMode(int taskHandle, int data);
		//		public int DAQmxResetSwitchScanRepeatMode(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SwitchScan_WaitingForAdv ***
		//		public int DAQmxGetSwitchScanWaitingForAdv(int taskHandle, IntByReference data);
		//
		//		//********** Scale **********
		//		//*** Set/Get functions for DAQmx_Scale_Descr ***
		//		public int DAQmxGetScaleDescr(String scaleName, byte[] data, int bufferSize);
		//		public int DAQmxSetScaleDescr(String scaleName, final byte[] data);
		//		//*** Set/Get functions for DAQmx_Scale_ScaledUnits ***
		//		public int DAQmxGetScaleScaledUnits(String scaleName, byte[] data, int bufferSize);
		//		public int DAQmxSetScaleScaledUnits(String scaleName, final byte[] data);
		//		//*** Set/Get functions for DAQmx_Scale_PreScaledUnits ***
		//		// Uses value set UnitsPreScaled
		//		public int DAQmxGetScalePreScaledUnits(String scaleName, IntByReference data);
		//		public int DAQmxSetScalePreScaledUnits(String scaleName, int data);
		//		//*** Set/Get functions for DAQmx_Scale_Type ***
		//		// Uses value set ScaleType
		//		public int DAQmxGetScaleType(String scaleName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Scale_Lin_Slope ***
		//		public int DAQmxGetScaleLinSlope(String scaleName, DoubleByReference data);
		//		public int DAQmxSetScaleLinSlope(String scaleName, double data);
		//		//*** Set/Get functions for DAQmx_Scale_Lin_YIntercept ***
		//		public int DAQmxGetScaleLinYIntercept(String scaleName, DoubleByReference data);
		//		public int DAQmxSetScaleLinYIntercept(String scaleName, double data);
		//		//*** Set/Get functions for DAQmx_Scale_Map_ScaledMax ***
		//		public int DAQmxGetScaleMapScaledMax(String scaleName, DoubleByReference data);
		//		public int DAQmxSetScaleMapScaledMax(String scaleName, double data);
		//		//*** Set/Get functions for DAQmx_Scale_Map_PreScaledMax ***
		//		public int DAQmxGetScaleMapPreScaledMax(String scaleName, DoubleByReference data);
		//		public int DAQmxSetScaleMapPreScaledMax(String scaleName, double data);
		//		//*** Set/Get functions for DAQmx_Scale_Map_ScaledMin ***
		//		public int DAQmxGetScaleMapScaledMin(String scaleName, DoubleByReference data);
		//		public int DAQmxSetScaleMapScaledMin(String scaleName, double data);
		//		//*** Set/Get functions for DAQmx_Scale_Map_PreScaledMin ***
		//		public int DAQmxGetScaleMapPreScaledMin(String scaleName, DoubleByReference data);
		//		public int DAQmxSetScaleMapPreScaledMin(String scaleName, double data);
		//		//*** Set/Get functions for DAQmx_Scale_Poly_ForwardCoeff ***
		//		public int DAQmxGetScalePolyForwardCoeff(String scaleName, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetScalePolyForwardCoeff(String scaleName, double[] data, int arraySizeInSamples);
		//		//*** Set/Get functions for DAQmx_Scale_Poly_ReverseCoeff ***
		//		public int DAQmxGetScalePolyReverseCoeff(String scaleName, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetScalePolyReverseCoeff(String scaleName, double[] data, int arraySizeInSamples);
		//		//*** Set/Get functions for DAQmx_Scale_Table_ScaledVals ***
		//		public int DAQmxGetScaleTableScaledVals(String scaleName, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetScaleTableScaledVals(String scaleName, double[] data, int arraySizeInSamples);
		//		//*** Set/Get functions for DAQmx_Scale_Table_PreScaledVals ***
		//		public int DAQmxGetScaleTablePreScaledVals(String scaleName, double[] data, int arraySizeInSamples);
		//		public int DAQmxSetScaleTablePreScaledVals(String scaleName, double[] data, int arraySizeInSamples);
		//
		//********** System **********
		//*** Set/Get functions for DAQmx_Sys_GlobalChans ***
		public int DAQmxGetSysGlobalChans(byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Sys_Scales ***
		public int DAQmxGetSysScales(byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Sys_Tasks ***
		public int DAQmxGetSysTasks(byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Sys_DevNames ***
		public int DAQmxGetSysDevNames(byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Sys_NIDAQMajorVersion ***
		public int DAQmxGetSysNIDAQMajorVersion(IntByReference data);
		//*** Set/Get functions for DAQmx_Sys_NIDAQMinorVersion ***
		public int DAQmxGetSysNIDAQMinorVersion(IntByReference data);

		//********** Task **********
		//*** Set/Get functions for DAQmx_Task_Name ***
		public int DAQmxGetTaskName(int taskHandle, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Task_Channels ***
		public int DAQmxGetTaskChannels(int taskHandle, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Task_NumChans ***
		public int DAQmxGetTaskNumChans(int taskHandle, IntByReference data);
		//*** Set/Get functions for DAQmx_Task_Devices ***
		public int DAQmxGetTaskDevices(int taskHandle, byte[] data, int bufferSize);
		//*** Set/Get functions for DAQmx_Task_NumDevices ***
		public int DAQmxGetTaskNumDevices(int taskHandle, IntByReference data);
		//*** Set/Get functions for DAQmx_Task_Complete ***
		public int DAQmxGetTaskComplete(int taskHandle, IntByReference data);

		//		//********** Timing **********
		//		//*** Set/Get functions for DAQmx_SampQuant_SampMode ***
		//		// Uses value set AcquisitionType
		//		public int DAQmxGetSampQuantSampMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampQuantSampMode(int taskHandle, int data);
		//		public int DAQmxResetSampQuantSampMode(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampQuant_SampPerChan ***
		//		public int DAQmxGetSampQuantSampPerChan(int taskHandle, LongByReference data);
		//		public int DAQmxSetSampQuantSampPerChan(int taskHandle, long data);
		//		public int DAQmxResetSampQuantSampPerChan(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampTimingType ***
		//		// Uses value set SampleTimingType
		//		public int DAQmxGetSampTimingType(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampTimingType(int taskHandle, int data);
		//		public int DAQmxResetSampTimingType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_Rate ***
		//		public int DAQmxGetSampClkRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetSampClkRate(int taskHandle, double data);
		//		public int DAQmxResetSampClkRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_MaxRate ***
		//		public int DAQmxGetSampClkMaxRate(int taskHandle, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SampClk_Src ***
		//		public int DAQmxGetSampClkSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetSampClkSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetSampClkSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_ActiveEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetSampClkActiveEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkActiveEdge(int taskHandle, int data);
		//		public int DAQmxResetSampClkActiveEdge(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_UnderflowBehavior ***
		//		// Uses value set UnderflowBehavior
		//		public int DAQmxGetSampClkUnderflowBehavior(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkUnderflowBehavior(int taskHandle, int data);
		//		public int DAQmxResetSampClkUnderflowBehavior(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_TimebaseDiv ***
		//		public int DAQmxGetSampClkTimebaseDiv(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkTimebaseDiv(int taskHandle, int data);
		//		public int DAQmxResetSampClkTimebaseDiv(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_Timebase_Rate ***
		//		public int DAQmxGetSampClkTimebaseRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetSampClkTimebaseRate(int taskHandle, double data);
		//		public int DAQmxResetSampClkTimebaseRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_Timebase_Src ***
		//		public int DAQmxGetSampClkTimebaseSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetSampClkTimebaseSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetSampClkTimebaseSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_Timebase_ActiveEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetSampClkTimebaseActiveEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkTimebaseActiveEdge(int taskHandle, int data);
		//		public int DAQmxResetSampClkTimebaseActiveEdge(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_Timebase_MasterTimebaseDiv ***
		//		public int DAQmxGetSampClkTimebaseMasterTimebaseDiv(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkTimebaseMasterTimebaseDiv(int taskHandle, int data);
		//		public int DAQmxResetSampClkTimebaseMasterTimebaseDiv(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_DigFltr_Enable ***
		//		public int DAQmxGetSampClkDigFltrEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkDigFltrEnable(int taskHandle, int data);
		//		public int DAQmxResetSampClkDigFltrEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetSampClkDigFltrMinPulseWidth(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetSampClkDigFltrMinPulseWidth(int taskHandle, double data);
		//		public int DAQmxResetSampClkDigFltrMinPulseWidth(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetSampClkDigFltrTimebaseSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetSampClkDigFltrTimebaseSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetSampClkDigFltrTimebaseSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_DigFltr_TimebaseRate ***
		//		public int DAQmxGetSampClkDigFltrTimebaseRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetSampClkDigFltrTimebaseRate(int taskHandle, double data);
		//		public int DAQmxResetSampClkDigFltrTimebaseRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampClk_DigSync_Enable ***
		//		public int DAQmxGetSampClkDigSyncEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkDigSyncEnable(int taskHandle, int data);
		//		public int DAQmxResetSampClkDigSyncEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Hshk_DelayAfterXfer ***
		//		public int DAQmxGetHshkDelayAfterXfer(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetHshkDelayAfterXfer(int taskHandle, double data);
		//		public int DAQmxResetHshkDelayAfterXfer(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Hshk_StartCond ***
		//		// Uses value set HandshakeStartCondition
		//		public int DAQmxGetHshkStartCond(int taskHandle, IntByReference data);
		//		public int DAQmxSetHshkStartCond(int taskHandle, int data);
		//		public int DAQmxResetHshkStartCond(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Hshk_SampleInputDataWhen ***
		//		// Uses value set SampleInputDataWhen
		//		public int DAQmxGetHshkSampleInputDataWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetHshkSampleInputDataWhen(int taskHandle, int data);
		//		public int DAQmxResetHshkSampleInputDataWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_ChangeDetect_DI_RisingEdgePhysicalChans ***
		//		public int DAQmxGetChangeDetectDIRisingEdgePhysicalChans(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetChangeDetectDIRisingEdgePhysicalChans(int taskHandle, final byte[] data);
		//		public int DAQmxResetChangeDetectDIRisingEdgePhysicalChans(int taskHandle);
		//		//*** Set/Get functions for DAQmx_ChangeDetect_DI_FallingEdgePhysicalChans ***
		//		public int DAQmxGetChangeDetectDIFallingEdgePhysicalChans(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetChangeDetectDIFallingEdgePhysicalChans(int taskHandle, final byte[] data);
		//		public int DAQmxResetChangeDetectDIFallingEdgePhysicalChans(int taskHandle);
		//		//*** Set/Get functions for DAQmx_OnDemand_SimultaneousAOEnable ***
		//		public int DAQmxGetOnDemandSimultaneousAOEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetOnDemandSimultaneousAOEnable(int taskHandle, int data);
		//		public int DAQmxResetOnDemandSimultaneousAOEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AIConv_Rate ***
		//		public int DAQmxGetAIConvRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAIConvRate(int taskHandle, double data);
		//		public int DAQmxResetAIConvRate(int taskHandle);
		//		public int DAQmxGetAIConvRateEx(int taskHandle, String deviceNames, DoubleByReference data);
		//		public int DAQmxSetAIConvRateEx(int taskHandle, String deviceNames, double data);
		//		public int DAQmxResetAIConvRateEx(int taskHandle, String deviceNames);
		//		//*** Set/Get functions for DAQmx_AIConv_MaxRate ***
		//		public int DAQmxGetAIConvMaxRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxGetAIConvMaxRateEx(int taskHandle, String deviceNames, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_AIConv_Src ***
		//		public int DAQmxGetAIConvSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetAIConvSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetAIConvSrc(int taskHandle);
		//		public int DAQmxGetAIConvSrcEx(int taskHandle, String deviceNames, byte[] data, int bufferSize);
		//		public int DAQmxSetAIConvSrcEx(int taskHandle, String deviceNames, final byte[] data);
		//		public int DAQmxResetAIConvSrcEx(int taskHandle, String deviceNames);
		//		//*** Set/Get functions for DAQmx_AIConv_ActiveEdge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetAIConvActiveEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetAIConvActiveEdge(int taskHandle, int data);
		//		public int DAQmxResetAIConvActiveEdge(int taskHandle);
		//		public int DAQmxGetAIConvActiveEdgeEx(int taskHandle, String deviceNames, IntByReference data);
		//		public int DAQmxSetAIConvActiveEdgeEx(int taskHandle, String deviceNames, int data);
		//		public int DAQmxResetAIConvActiveEdgeEx(int taskHandle, String deviceNames);
		//		//*** Set/Get functions for DAQmx_AIConv_TimebaseDiv ***
		//		public int DAQmxGetAIConvTimebaseDiv(int taskHandle, IntByReference data);
		//		public int DAQmxSetAIConvTimebaseDiv(int taskHandle, int data);
		//		public int DAQmxResetAIConvTimebaseDiv(int taskHandle);
		//		public int DAQmxGetAIConvTimebaseDivEx(int taskHandle, String deviceNames, IntByReference data);
		//		public int DAQmxSetAIConvTimebaseDivEx(int taskHandle, String deviceNames, int data);
		//		public int DAQmxResetAIConvTimebaseDivEx(int taskHandle, String deviceNames);
		//		//*** Set/Get functions for DAQmx_AIConv_Timebase_Src ***
		//		// Uses value set MIOAIConvertTbSrc
		//		public int DAQmxGetAIConvTimebaseSrc(int taskHandle, IntByReference data);
		//		public int DAQmxSetAIConvTimebaseSrc(int taskHandle, int data);
		//		public int DAQmxResetAIConvTimebaseSrc(int taskHandle);
		//		public int DAQmxGetAIConvTimebaseSrcEx(int taskHandle, String deviceNames, IntByReference data);
		//		public int DAQmxSetAIConvTimebaseSrcEx(int taskHandle, String deviceNames, int data);
		//		public int DAQmxResetAIConvTimebaseSrcEx(int taskHandle, String deviceNames);
		//		//*** Set/Get functions for DAQmx_DelayFromSampClk_DelayUnits ***
		//		// Uses value set DigitalWidthUnits2
		//		public int DAQmxGetDelayFromSampClkDelayUnits(int taskHandle, IntByReference data);
		//		public int DAQmxSetDelayFromSampClkDelayUnits(int taskHandle, int data);
		//		public int DAQmxResetDelayFromSampClkDelayUnits(int taskHandle);
		//		public int DAQmxGetDelayFromSampClkDelayUnitsEx(int taskHandle, String deviceNames, IntByReference data);
		//		public int DAQmxSetDelayFromSampClkDelayUnitsEx(int taskHandle, String deviceNames, int data);
		//		public int DAQmxResetDelayFromSampClkDelayUnitsEx(int taskHandle, String deviceNames);
		//		//*** Set/Get functions for DAQmx_DelayFromSampClk_Delay ***
		//		public int DAQmxGetDelayFromSampClkDelay(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetDelayFromSampClkDelay(int taskHandle, double data);
		//		public int DAQmxResetDelayFromSampClkDelay(int taskHandle);
		//		public int DAQmxGetDelayFromSampClkDelayEx(int taskHandle, String deviceNames, DoubleByReference data);
		//		public int DAQmxSetDelayFromSampClkDelayEx(int taskHandle, String deviceNames, double data);
		//		public int DAQmxResetDelayFromSampClkDelayEx(int taskHandle, String deviceNames);
		//		//*** Set/Get functions for DAQmx_MasterTimebase_Rate ***
		//		public int DAQmxGetMasterTimebaseRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetMasterTimebaseRate(int taskHandle, double data);
		//		public int DAQmxResetMasterTimebaseRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_MasterTimebase_Src ***
		//		public int DAQmxGetMasterTimebaseSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetMasterTimebaseSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetMasterTimebaseSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RefClk_Rate ***
		//		public int DAQmxGetRefClkRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetRefClkRate(int taskHandle, double data);
		//		public int DAQmxResetRefClkRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RefClk_Src ***
		//		public int DAQmxGetRefClkSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetRefClkSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetRefClkSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SyncPulse_Src ***
		//		public int DAQmxGetSyncPulseSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetSyncPulseSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetSyncPulseSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SyncPulse_SyncTime ***
		//		public int DAQmxGetSyncPulseSyncTime(int taskHandle, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_SyncPulse_MinDelayToStart ***
		//		public int DAQmxGetSyncPulseMinDelayToStart(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetSyncPulseMinDelayToStart(int taskHandle, double data);
		//		public int DAQmxResetSyncPulseMinDelayToStart(int taskHandle);
		//		//*** Set/Get functions for DAQmx_SampTimingEngine ***
		//		public int DAQmxGetSampTimingEngine(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampTimingEngine(int taskHandle, int data);
		//		public int DAQmxResetSampTimingEngine(int taskHandle);
		//
		//		//********** Trigger **********
		//		//*** Set/Get functions for DAQmx_StartTrig_Type ***
		//		// Uses value set TriggerType8
		//		public int DAQmxGetStartTrigType(int taskHandle, IntByReference data);
		//		public int DAQmxSetStartTrigType(int taskHandle, int data);
		//		public int DAQmxResetStartTrigType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_StartTrig_Src ***
		//		public int DAQmxGetDigEdgeStartTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigEdgeStartTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigEdgeStartTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_StartTrig_Edge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetDigEdgeStartTrigEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeStartTrigEdge(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeStartTrigEdge(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_StartTrig_DigFltr_Enable ***
		//		public int DAQmxGetDigEdgeStartTrigDigFltrEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeStartTrigDigFltrEnable(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeStartTrigDigFltrEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_StartTrig_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetDigEdgeStartTrigDigFltrMinPulseWidth(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetDigEdgeStartTrigDigFltrMinPulseWidth(int taskHandle, double data);
		//		public int DAQmxResetDigEdgeStartTrigDigFltrMinPulseWidth(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_StartTrig_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetDigEdgeStartTrigDigFltrTimebaseSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigEdgeStartTrigDigFltrTimebaseSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigEdgeStartTrigDigFltrTimebaseSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_StartTrig_DigFltr_TimebaseRate ***
		//		public int DAQmxGetDigEdgeStartTrigDigFltrTimebaseRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetDigEdgeStartTrigDigFltrTimebaseRate(int taskHandle, double data);
		//		public int DAQmxResetDigEdgeStartTrigDigFltrTimebaseRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_StartTrig_DigSync_Enable ***
		//		public int DAQmxGetDigEdgeStartTrigDigSyncEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeStartTrigDigSyncEnable(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeStartTrigDigSyncEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_StartTrig_Src ***
		//		public int DAQmxGetDigPatternStartTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigPatternStartTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigPatternStartTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_StartTrig_Pattern ***
		//		public int DAQmxGetDigPatternStartTrigPattern(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigPatternStartTrigPattern(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigPatternStartTrigPattern(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_StartTrig_When ***
		//		// Uses value set DigitalPatternCondition1
		//		public int DAQmxGetDigPatternStartTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigPatternStartTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetDigPatternStartTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_StartTrig_Src ***
		//		public int DAQmxGetAnlgEdgeStartTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetAnlgEdgeStartTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetAnlgEdgeStartTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_StartTrig_Slope ***
		//		// Uses value set Slope1
		//		public int DAQmxGetAnlgEdgeStartTrigSlope(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgEdgeStartTrigSlope(int taskHandle, int data);
		//		public int DAQmxResetAnlgEdgeStartTrigSlope(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_StartTrig_Lvl ***
		//		public int DAQmxGetAnlgEdgeStartTrigLvl(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgEdgeStartTrigLvl(int taskHandle, double data);
		//		public int DAQmxResetAnlgEdgeStartTrigLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_StartTrig_Hyst ***
		//		public int DAQmxGetAnlgEdgeStartTrigHyst(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgEdgeStartTrigHyst(int taskHandle, double data);
		//		public int DAQmxResetAnlgEdgeStartTrigHyst(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_StartTrig_Coupling ***
		//		// Uses value set Coupling2
		//		public int DAQmxGetAnlgEdgeStartTrigCoupling(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgEdgeStartTrigCoupling(int taskHandle, int data);
		//		public int DAQmxResetAnlgEdgeStartTrigCoupling(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_StartTrig_Src ***
		//		public int DAQmxGetAnlgWinStartTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetAnlgWinStartTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetAnlgWinStartTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_StartTrig_When ***
		//		// Uses value set WindowTriggerCondition1
		//		public int DAQmxGetAnlgWinStartTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgWinStartTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetAnlgWinStartTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_StartTrig_Top ***
		//		public int DAQmxGetAnlgWinStartTrigTop(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgWinStartTrigTop(int taskHandle, double data);
		//		public int DAQmxResetAnlgWinStartTrigTop(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_StartTrig_Btm ***
		//		public int DAQmxGetAnlgWinStartTrigBtm(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgWinStartTrigBtm(int taskHandle, double data);
		//		public int DAQmxResetAnlgWinStartTrigBtm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_StartTrig_Coupling ***
		//		// Uses value set Coupling2
		//		public int DAQmxGetAnlgWinStartTrigCoupling(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgWinStartTrigCoupling(int taskHandle, int data);
		//		public int DAQmxResetAnlgWinStartTrigCoupling(int taskHandle);
		//		//*** Set/Get functions for DAQmx_StartTrig_Delay ***
		//		public int DAQmxGetStartTrigDelay(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetStartTrigDelay(int taskHandle, double data);
		//		public int DAQmxResetStartTrigDelay(int taskHandle);
		//		//*** Set/Get functions for DAQmx_StartTrig_DelayUnits ***
		//		// Uses value set DigitalWidthUnits1
		//		public int DAQmxGetStartTrigDelayUnits(int taskHandle, IntByReference data);
		//		public int DAQmxSetStartTrigDelayUnits(int taskHandle, int data);
		//		public int DAQmxResetStartTrigDelayUnits(int taskHandle);
		//		//*** Set/Get functions for DAQmx_StartTrig_Retriggerable ***
		//		public int DAQmxGetStartTrigRetriggerable(int taskHandle, IntByReference data);
		//		public int DAQmxSetStartTrigRetriggerable(int taskHandle, int data);
		//		public int DAQmxResetStartTrigRetriggerable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RefTrig_Type ***
		//		// Uses value set TriggerType8
		//		public int DAQmxGetRefTrigType(int taskHandle, IntByReference data);
		//		public int DAQmxSetRefTrigType(int taskHandle, int data);
		//		public int DAQmxResetRefTrigType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_RefTrig_PretrigSamples ***
		//		public int DAQmxGetRefTrigPretrigSamples(int taskHandle, IntByReference data);
		//		public int DAQmxSetRefTrigPretrigSamples(int taskHandle, int data);
		//		public int DAQmxResetRefTrigPretrigSamples(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_RefTrig_Src ***
		//		public int DAQmxGetDigEdgeRefTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigEdgeRefTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigEdgeRefTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_RefTrig_Edge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetDigEdgeRefTrigEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeRefTrigEdge(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeRefTrigEdge(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_RefTrig_Src ***
		//		public int DAQmxGetDigPatternRefTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigPatternRefTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigPatternRefTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_RefTrig_Pattern ***
		//		public int DAQmxGetDigPatternRefTrigPattern(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigPatternRefTrigPattern(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigPatternRefTrigPattern(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_RefTrig_When ***
		//		// Uses value set DigitalPatternCondition1
		//		public int DAQmxGetDigPatternRefTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigPatternRefTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetDigPatternRefTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_RefTrig_Src ***
		//		public int DAQmxGetAnlgEdgeRefTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetAnlgEdgeRefTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetAnlgEdgeRefTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_RefTrig_Slope ***
		//		// Uses value set Slope1
		//		public int DAQmxGetAnlgEdgeRefTrigSlope(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgEdgeRefTrigSlope(int taskHandle, int data);
		//		public int DAQmxResetAnlgEdgeRefTrigSlope(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_RefTrig_Lvl ***
		//		public int DAQmxGetAnlgEdgeRefTrigLvl(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgEdgeRefTrigLvl(int taskHandle, double data);
		//		public int DAQmxResetAnlgEdgeRefTrigLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_RefTrig_Hyst ***
		//		public int DAQmxGetAnlgEdgeRefTrigHyst(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgEdgeRefTrigHyst(int taskHandle, double data);
		//		public int DAQmxResetAnlgEdgeRefTrigHyst(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgEdge_RefTrig_Coupling ***
		//		// Uses value set Coupling2
		//		public int DAQmxGetAnlgEdgeRefTrigCoupling(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgEdgeRefTrigCoupling(int taskHandle, int data);
		//		public int DAQmxResetAnlgEdgeRefTrigCoupling(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_RefTrig_Src ***
		//		public int DAQmxGetAnlgWinRefTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetAnlgWinRefTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetAnlgWinRefTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_RefTrig_When ***
		//		// Uses value set WindowTriggerCondition1
		//		public int DAQmxGetAnlgWinRefTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgWinRefTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetAnlgWinRefTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_RefTrig_Top ***
		//		public int DAQmxGetAnlgWinRefTrigTop(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgWinRefTrigTop(int taskHandle, double data);
		//		public int DAQmxResetAnlgWinRefTrigTop(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_RefTrig_Btm ***
		//		public int DAQmxGetAnlgWinRefTrigBtm(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgWinRefTrigBtm(int taskHandle, double data);
		//		public int DAQmxResetAnlgWinRefTrigBtm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_RefTrig_Coupling ***
		//		// Uses value set Coupling2
		//		public int DAQmxGetAnlgWinRefTrigCoupling(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgWinRefTrigCoupling(int taskHandle, int data);
		//		public int DAQmxResetAnlgWinRefTrigCoupling(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AdvTrig_Type ***
		//		// Uses value set TriggerType5
		//		public int DAQmxGetAdvTrigType(int taskHandle, IntByReference data);
		//		public int DAQmxSetAdvTrigType(int taskHandle, int data);
		//		public int DAQmxResetAdvTrigType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_AdvTrig_Src ***
		//		public int DAQmxGetDigEdgeAdvTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigEdgeAdvTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigEdgeAdvTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_AdvTrig_Edge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetDigEdgeAdvTrigEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeAdvTrigEdge(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeAdvTrigEdge(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_AdvTrig_DigFltr_Enable ***
		//		public int DAQmxGetDigEdgeAdvTrigDigFltrEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeAdvTrigDigFltrEnable(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeAdvTrigDigFltrEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_HshkTrig_Type ***
		//		// Uses value set TriggerType9
		//		public int DAQmxGetHshkTrigType(int taskHandle, IntByReference data);
		//		public int DAQmxSetHshkTrigType(int taskHandle, int data);
		//		public int DAQmxResetHshkTrigType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Interlocked_HshkTrig_Src ***
		//		public int DAQmxGetInterlockedHshkTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetInterlockedHshkTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetInterlockedHshkTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Interlocked_HshkTrig_AssertedLvl ***
		//		// Uses value set Level1
		//		public int DAQmxGetInterlockedHshkTrigAssertedLvl(int taskHandle, IntByReference data);
		//		public int DAQmxSetInterlockedHshkTrigAssertedLvl(int taskHandle, int data);
		//		public int DAQmxResetInterlockedHshkTrigAssertedLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_PauseTrig_Type ***
		//		// Uses value set TriggerType6
		//		public int DAQmxGetPauseTrigType(int taskHandle, IntByReference data);
		//		public int DAQmxSetPauseTrigType(int taskHandle, int data);
		//		public int DAQmxResetPauseTrigType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgLvl_PauseTrig_Src ***
		//		public int DAQmxGetAnlgLvlPauseTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetAnlgLvlPauseTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetAnlgLvlPauseTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgLvl_PauseTrig_When ***
		//		// Uses value set ActiveLevel
		//		public int DAQmxGetAnlgLvlPauseTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgLvlPauseTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetAnlgLvlPauseTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgLvl_PauseTrig_Lvl ***
		//		public int DAQmxGetAnlgLvlPauseTrigLvl(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgLvlPauseTrigLvl(int taskHandle, double data);
		//		public int DAQmxResetAnlgLvlPauseTrigLvl(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgLvl_PauseTrig_Hyst ***
		//		public int DAQmxGetAnlgLvlPauseTrigHyst(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgLvlPauseTrigHyst(int taskHandle, double data);
		//		public int DAQmxResetAnlgLvlPauseTrigHyst(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgLvl_PauseTrig_Coupling ***
		//		// Uses value set Coupling2
		//		public int DAQmxGetAnlgLvlPauseTrigCoupling(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgLvlPauseTrigCoupling(int taskHandle, int data);
		//		public int DAQmxResetAnlgLvlPauseTrigCoupling(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_PauseTrig_Src ***
		//		public int DAQmxGetAnlgWinPauseTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetAnlgWinPauseTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetAnlgWinPauseTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_PauseTrig_When ***
		//		// Uses value set WindowTriggerCondition2
		//		public int DAQmxGetAnlgWinPauseTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgWinPauseTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetAnlgWinPauseTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_PauseTrig_Top ***
		//		public int DAQmxGetAnlgWinPauseTrigTop(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgWinPauseTrigTop(int taskHandle, double data);
		//		public int DAQmxResetAnlgWinPauseTrigTop(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_PauseTrig_Btm ***
		//		public int DAQmxGetAnlgWinPauseTrigBtm(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetAnlgWinPauseTrigBtm(int taskHandle, double data);
		//		public int DAQmxResetAnlgWinPauseTrigBtm(int taskHandle);
		//		//*** Set/Get functions for DAQmx_AnlgWin_PauseTrig_Coupling ***
		//		// Uses value set Coupling2
		//		public int DAQmxGetAnlgWinPauseTrigCoupling(int taskHandle, IntByReference data);
		//		public int DAQmxSetAnlgWinPauseTrigCoupling(int taskHandle, int data);
		//		public int DAQmxResetAnlgWinPauseTrigCoupling(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigLvl_PauseTrig_Src ***
		//		public int DAQmxGetDigLvlPauseTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigLvlPauseTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigLvlPauseTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigLvl_PauseTrig_When ***
		//		// Uses value set Level1
		//		public int DAQmxGetDigLvlPauseTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigLvlPauseTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetDigLvlPauseTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigLvl_PauseTrig_DigFltr_Enable ***
		//		public int DAQmxGetDigLvlPauseTrigDigFltrEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigLvlPauseTrigDigFltrEnable(int taskHandle, int data);
		//		public int DAQmxResetDigLvlPauseTrigDigFltrEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigLvl_PauseTrig_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetDigLvlPauseTrigDigFltrMinPulseWidth(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetDigLvlPauseTrigDigFltrMinPulseWidth(int taskHandle, double data);
		//		public int DAQmxResetDigLvlPauseTrigDigFltrMinPulseWidth(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigLvl_PauseTrig_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetDigLvlPauseTrigDigFltrTimebaseSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigLvlPauseTrigDigFltrTimebaseSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigLvlPauseTrigDigFltrTimebaseSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigLvl_PauseTrig_DigFltr_TimebaseRate ***
		//		public int DAQmxGetDigLvlPauseTrigDigFltrTimebaseRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetDigLvlPauseTrigDigFltrTimebaseRate(int taskHandle, double data);
		//		public int DAQmxResetDigLvlPauseTrigDigFltrTimebaseRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigLvl_PauseTrig_DigSync_Enable ***
		//		public int DAQmxGetDigLvlPauseTrigDigSyncEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigLvlPauseTrigDigSyncEnable(int taskHandle, int data);
		//		public int DAQmxResetDigLvlPauseTrigDigSyncEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_PauseTrig_Src ***
		//		public int DAQmxGetDigPatternPauseTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigPatternPauseTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigPatternPauseTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_PauseTrig_Pattern ***
		//		public int DAQmxGetDigPatternPauseTrigPattern(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigPatternPauseTrigPattern(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigPatternPauseTrigPattern(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigPattern_PauseTrig_When ***
		//		// Uses value set DigitalPatternCondition1
		//		public int DAQmxGetDigPatternPauseTrigWhen(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigPatternPauseTrigWhen(int taskHandle, int data);
		//		public int DAQmxResetDigPatternPauseTrigWhen(int taskHandle);
		//		//*** Set/Get functions for DAQmx_ArmStartTrig_Type ***
		//		// Uses value set TriggerType4
		//		public int DAQmxGetArmStartTrigType(int taskHandle, IntByReference data);
		//		public int DAQmxSetArmStartTrigType(int taskHandle, int data);
		//		public int DAQmxResetArmStartTrigType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_ArmStartTrig_Src ***
		//		public int DAQmxGetDigEdgeArmStartTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigEdgeArmStartTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigEdgeArmStartTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_ArmStartTrig_Edge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetDigEdgeArmStartTrigEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeArmStartTrigEdge(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeArmStartTrigEdge(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_ArmStartTrig_DigFltr_Enable ***
		//		public int DAQmxGetDigEdgeArmStartTrigDigFltrEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeArmStartTrigDigFltrEnable(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeArmStartTrigDigFltrEnable(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_ArmStartTrig_DigFltr_MinPulseWidth ***
		//		public int DAQmxGetDigEdgeArmStartTrigDigFltrMinPulseWidth(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetDigEdgeArmStartTrigDigFltrMinPulseWidth(int taskHandle, double data);
		//		public int DAQmxResetDigEdgeArmStartTrigDigFltrMinPulseWidth(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_ArmStartTrig_DigFltr_TimebaseSrc ***
		//		public int DAQmxGetDigEdgeArmStartTrigDigFltrTimebaseSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigEdgeArmStartTrigDigFltrTimebaseSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigEdgeArmStartTrigDigFltrTimebaseSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_ArmStartTrig_DigFltr_TimebaseRate ***
		//		public int DAQmxGetDigEdgeArmStartTrigDigFltrTimebaseRate(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetDigEdgeArmStartTrigDigFltrTimebaseRate(int taskHandle, double data);
		//		public int DAQmxResetDigEdgeArmStartTrigDigFltrTimebaseRate(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_ArmStartTrig_DigSync_Enable ***
		//		public int DAQmxGetDigEdgeArmStartTrigDigSyncEnable(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeArmStartTrigDigSyncEnable(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeArmStartTrigDigSyncEnable(int taskHandle);
		//
		//		//********** Watchdog **********
		//		//*** Set/Get functions for DAQmx_Watchdog_Timeout ***
		//		public int DAQmxGetWatchdogTimeout(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetWatchdogTimeout(int taskHandle, double data);
		//		public int DAQmxResetWatchdogTimeout(int taskHandle);
		//		//*** Set/Get functions for DAQmx_WatchdogExpirTrig_Type ***
		//		// Uses value set TriggerType4
		//		public int DAQmxGetWatchdogExpirTrigType(int taskHandle, IntByReference data);
		//		public int DAQmxSetWatchdogExpirTrigType(int taskHandle, int data);
		//		public int DAQmxResetWatchdogExpirTrigType(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_WatchdogExpirTrig_Src ***
		//		public int DAQmxGetDigEdgeWatchdogExpirTrigSrc(int taskHandle, byte[] data, int bufferSize);
		//		public int DAQmxSetDigEdgeWatchdogExpirTrigSrc(int taskHandle, final byte[] data);
		//		public int DAQmxResetDigEdgeWatchdogExpirTrigSrc(int taskHandle);
		//		//*** Set/Get functions for DAQmx_DigEdge_WatchdogExpirTrig_Edge ***
		//		// Uses value set Edge1
		//		public int DAQmxGetDigEdgeWatchdogExpirTrigEdge(int taskHandle, IntByReference data);
		//		public int DAQmxSetDigEdgeWatchdogExpirTrigEdge(int taskHandle, int data);
		//		public int DAQmxResetDigEdgeWatchdogExpirTrigEdge(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Watchdog_DO_ExpirState ***
		//		// Uses value set DigitalLineState
		//		public int DAQmxGetWatchdogDOExpirState(int taskHandle, String lines, IntByReference data);
		//		public int DAQmxSetWatchdogDOExpirState(int taskHandle, String lines, int data);
		//		public int DAQmxResetWatchdogDOExpirState(int taskHandle, String lines);
		//		//*** Set/Get functions for DAQmx_Watchdog_HasExpired ***
		//		public int DAQmxGetWatchdogHasExpired(int taskHandle, IntByReference data);
		//
		//		//********** Write **********
		//		//*** Set/Get functions for DAQmx_Write_RelativeTo ***
		//		// Uses value set WriteRelativeTo
		//		public int DAQmxGetWriteRelativeTo(int taskHandle, IntByReference data);
		//		public int DAQmxSetWriteRelativeTo(int taskHandle, int data);
		//		public int DAQmxResetWriteRelativeTo(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Write_Offset ***
		//		public int DAQmxGetWriteOffset(int taskHandle, IntByReference data);
		//		public int DAQmxSetWriteOffset(int taskHandle, int data);
		//		public int DAQmxResetWriteOffset(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Write_RegenMode ***
		//		// Uses value set RegenerationMode1
		//		public int DAQmxGetWriteRegenMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetWriteRegenMode(int taskHandle, int data);
		//		public int DAQmxResetWriteRegenMode(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Write_CurrWritePos ***
		//		public int DAQmxGetWriteCurrWritePos(int taskHandle, LongByReference data);
		//		//*** Set/Get functions for DAQmx_Write_OvercurrentChansExist ***
		//		public int DAQmxGetWriteOvercurrentChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Write_OvercurrentChans ***
		//		public int DAQmxGetWriteOvercurrentChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Write_OvertemperatureChansExist ***
		//		public int DAQmxGetWriteOvertemperatureChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Write_OpenCurrentLoopChansExist ***
		//		public int DAQmxGetWriteOpenCurrentLoopChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Write_OpenCurrentLoopChans ***
		//		public int DAQmxGetWriteOpenCurrentLoopChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Write_PowerSupplyFaultChansExist ***
		//		public int DAQmxGetWritePowerSupplyFaultChansExist(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Write_PowerSupplyFaultChans ***
		//		public int DAQmxGetWritePowerSupplyFaultChans(int taskHandle, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_Write_SpaceAvail ***
		//		public int DAQmxGetWriteSpaceAvail(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Write_TotalSampPerChanGenerated ***
		//		public int DAQmxGetWriteTotalSampPerChanGenerated(int taskHandle, LongByReference data);
		//		//*** Set/Get functions for DAQmx_Write_RawDataWidth ***
		//		public int DAQmxGetWriteRawDataWidth(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Write_NumChans ***
		//		public int DAQmxGetWriteNumChans(int taskHandle, IntByReference data);
		//		//*** Set/Get functions for DAQmx_Write_WaitMode ***
		//		// Uses value set WaitMode2
		//		public int DAQmxGetWriteWaitMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetWriteWaitMode(int taskHandle, int data);
		//		public int DAQmxResetWriteWaitMode(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Write_SleepTime ***
		//		public int DAQmxGetWriteSleepTime(int taskHandle, DoubleByReference data);
		//		public int DAQmxSetWriteSleepTime(int taskHandle, double data);
		//		public int DAQmxResetWriteSleepTime(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Write_NextWriteIsLast ***
		//		public int DAQmxGetWriteNextWriteIsLast(int taskHandle, IntByReference data);
		//		public int DAQmxSetWriteNextWriteIsLast(int taskHandle, int data);
		//		public int DAQmxResetWriteNextWriteIsLast(int taskHandle);
		//		//*** Set/Get functions for DAQmx_Write_DigitalLines_BytesPerChan ***
		//		public int DAQmxGetWriteDigitalLinesBytesPerChan(int taskHandle, IntByReference data);
		//
		//		//********** Physical Channel **********
		//		//*** Set/Get functions for DAQmx_PhysicalChan_AI_TermCfgs ***
		//		// Uses bits from enum TerminalConfigurationBits
		//		public int DAQmxGetPhysicalChanAITermCfgs(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_AO_TermCfgs ***
		//		// Uses bits from enum TerminalConfigurationBits
		//		public int DAQmxGetPhysicalChanAOTermCfgs(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_AO_ManualControlEnable ***
		//		public int DAQmxGetPhysicalChanAOManualControlEnable(String physicalChannel, IntByReference data);
		//		public int DAQmxSetPhysicalChanAOManualControlEnable(String physicalChannel, int data);
		//		public int DAQmxResetPhysicalChanAOManualControlEnable(String physicalChannel);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_AO_ManualControlAmplitude ***
		//		public int DAQmxGetPhysicalChanAOManualControlAmplitude(String physicalChannel, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_AO_ManualControlFreq ***
		//		public int DAQmxGetPhysicalChanAOManualControlFreq(String physicalChannel, DoubleByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_DI_PortWidth ***
		//		public int DAQmxGetPhysicalChanDIPortWidth(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_DI_SampClkSupported ***
		//		public int DAQmxGetPhysicalChanDISampClkSupported(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_DI_ChangeDetectSupported ***
		//		public int DAQmxGetPhysicalChanDIChangeDetectSupported(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_DO_PortWidth ***
		//		public int DAQmxGetPhysicalChanDOPortWidth(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_DO_SampClkSupported ***
		//		public int DAQmxGetPhysicalChanDOSampClkSupported(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_TEDS_MfgID ***
		//		public int DAQmxGetPhysicalChanTEDSMfgID(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_TEDS_ModelNum ***
		//		public int DAQmxGetPhysicalChanTEDSModelNum(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_TEDS_SerialNum ***
		//		public int DAQmxGetPhysicalChanTEDSSerialNum(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_TEDS_VersionNum ***
		//		public int DAQmxGetPhysicalChanTEDSVersionNum(String physicalChannel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_TEDS_VersionLetter ***
		//		public int DAQmxGetPhysicalChanTEDSVersionLetter(String physicalChannel, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_TEDS_BitStream ***
		//		public int DAQmxGetPhysicalChanTEDSBitStream(String physicalChannel, byte[] data, int arraySizeInSamples);
		//		//*** Set/Get functions for DAQmx_PhysicalChan_TEDS_TemplateIDs ***
		//		public int DAQmxGetPhysicalChanTEDSTemplateIDs(String physicalChannel, byte[] data, int arraySizeInSamples);
		//
		//		//********** Persisted Task **********
		//		//*** Set/Get functions for DAQmx_PersistedTask_Author ***
		//		public int DAQmxGetPersistedTaskAuthor(String taskName, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_PersistedTask_AllowInteractiveEditing ***
		//		public int DAQmxGetPersistedTaskAllowInteractiveEditing(String taskName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PersistedTask_AllowInteractiveDeletion ***
		//		public int DAQmxGetPersistedTaskAllowInteractiveDeletion(String taskName, IntByReference data);
		//
		//		//********** Persisted Channel **********
		//		//*** Set/Get functions for DAQmx_PersistedChan_Author ***
		//		public int DAQmxGetPersistedChanAuthor(String channel, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_PersistedChan_AllowInteractiveEditing ***
		//		public int DAQmxGetPersistedChanAllowInteractiveEditing(String channel, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PersistedChan_AllowInteractiveDeletion ***
		//		public int DAQmxGetPersistedChanAllowInteractiveDeletion(String channel, IntByReference data);
		//
		//		//********** Persisted Scale **********
		//		//*** Set/Get functions for DAQmx_PersistedScale_Author ***
		//		public int DAQmxGetPersistedScaleAuthor(String scaleName, byte[] data, int bufferSize);
		//		//*** Set/Get functions for DAQmx_PersistedScale_AllowInteractiveEditing ***
		//		public int DAQmxGetPersistedScaleAllowInteractiveEditing(String scaleName, IntByReference data);
		//		//*** Set/Get functions for DAQmx_PersistedScale_AllowInteractiveDeletion ***
		//		public int DAQmxGetPersistedScaleAllowInteractiveDeletion(String scaleName, IntByReference data);
		//
		//		//*** Set/Get functions for DAQmx_SampClk_TimingResponseMode ***
		//		// Uses value set TimingResponseMode
		//		// Obsolete - always returns 0
		//		public int DAQmxGetSampClkTimingResponseMode(int taskHandle, IntByReference data);
		//		public int DAQmxSetSampClkTimingResponseMode(int taskHandle, int data);
		//		public int DAQmxResetSampClkTimingResponseMode(int taskHandle);
		//


	}
}
