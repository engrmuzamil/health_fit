package cachet.plugins.health

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.fitness.result.SessionReadResponse
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Session
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.util.*
import java.util.concurrent.*

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1111
const val CHANNEL_NAME = "flutter_health"
const val MMOLL_2_MGDL = 18.0 // 1 mmoll= 18 mgdl

class HealthPlugin : MethodCallHandler, FlutterPlugin, ActivityAware, PluginRegistry.ActivityResultListener {
    private var channel: MethodChannel? = null
    private var result: Result? = null
    private var handler: Handler? = null
    private var activity: Activity? = null
    private var threadPoolExecutor: ExecutorService? = null
    private var activityBinding: ActivityPluginBinding? = null

    private var mResult: Result? = null

    private var BODY_FAT_PERCENTAGE = "BODY_FAT_PERCENTAGE"
    private var HEIGHT = "HEIGHT"
    private var WEIGHT = "WEIGHT"
    private var STEPS = "STEPS"
    private var AGGREGATE_STEP_COUNT = "AGGREGATE_STEP_COUNT"
    private var ACTIVE_ENERGY_BURNED = "ACTIVE_ENERGY_BURNED"
    private var HEART_RATE = "HEART_RATE"
    private var BODY_TEMPERATURE = "BODY_TEMPERATURE"
    private var BLOOD_PRESSURE_SYSTOLIC = "BLOOD_PRESSURE_SYSTOLIC"
    private var BLOOD_PRESSURE_DIASTOLIC = "BLOOD_PRESSURE_DIASTOLIC"
    private var BLOOD_OXYGEN = "BLOOD_OXYGEN"
    private var BLOOD_GLUCOSE = "BLOOD_GLUCOSE"
    private var MOVE_MINUTES = "MOVE_MINUTES"
    private var DISTANCE_DELTA = "DISTANCE_DELTA"
    private var WATER = "WATER"
    private var SLEEP_ASLEEP = "SLEEP_ASLEEP"
    private var SLEEP_AWAKE = "SLEEP_AWAKE"
    private var SLEEP_IN_BED = "SLEEP_IN_BED"
    private var WORKOUT = "WORKOUT"

    val workoutTypeMap = mapOf(
        "AEROBICS" to FitnessActivities.AEROBICS,
        "AMERICAN_FOOTBALL" to FitnessActivities.FOOTBALL_AMERICAN,
        "ARCHERY" to FitnessActivities.ARCHERY,
        "AUSTRALIAN_FOOTBALL" to FitnessActivities.FOOTBALL_AUSTRALIAN,
        "BADMINTON" to FitnessActivities.BADMINTON,
        "BASEBALL" to FitnessActivities.BASEBALL,
        "BASKETBALL" to FitnessActivities.BASKETBALL,
        "BIATHLON" to FitnessActivities.BIATHLON,
        "BIKING" to FitnessActivities.BIKING,
        "BOXING" to FitnessActivities.BOXING,
        "CALISTHENICS" to FitnessActivities.CALISTHENICS,
        "CIRCUIT_TRAINING" to FitnessActivities.CIRCUIT_TRAINING,
        "CRICKET" to FitnessActivities.CRICKET,
        "CROSS_COUNTRY_SKIING" to FitnessActivities.SKIING_CROSS_COUNTRY,
        "CROSS_FIT" to FitnessActivities.CROSSFIT,
        "CURLING" to FitnessActivities.CURLING,
        "DANCING" to FitnessActivities.DANCING,
        "DIVING" to FitnessActivities.DIVING,
        "DOWNHILL_SKIING" to FitnessActivities.SKIING_DOWNHILL,
        "ELEVATOR" to FitnessActivities.ELEVATOR,
        "ELLIPTICAL" to FitnessActivities.ELLIPTICAL,
        "ERGOMETER" to FitnessActivities.ERGOMETER,
        "ESCALATOR" to FitnessActivities.ESCALATOR,
        "FENCING" to FitnessActivities.FENCING,
        "FRISBEE_DISC" to FitnessActivities.FRISBEE_DISC,
        "GARDENING" to FitnessActivities.GARDENING,
        "GOLF" to FitnessActivities.GOLF,
        "GUIDED_BREATHING" to FitnessActivities.GUIDED_BREATHING,
        "GYMNASTICS" to FitnessActivities.GYMNASTICS,
        "HANDBALL" to FitnessActivities.HANDBALL,
        "HIGH_INTENSITY_INTERVAL_TRAINING" to FitnessActivities.HIGH_INTENSITY_INTERVAL_TRAINING,
        "HIKING" to FitnessActivities.HIKING,
        "HOCKEY" to FitnessActivities.HOCKEY,
        "HORSEBACK_RIDING" to FitnessActivities.HORSEBACK_RIDING,
        "HOUSEWORK" to FitnessActivities.HOUSEWORK,
        "IN_VEHICLE" to FitnessActivities.IN_VEHICLE,
        "INTERVAL_TRAINING" to FitnessActivities.INTERVAL_TRAINING,
        "JUMP_ROPE" to FitnessActivities.JUMP_ROPE,
        "KAYAKING" to FitnessActivities.KAYAKING,
        "KETTLEBELL_TRAINING" to FitnessActivities.KETTLEBELL_TRAINING,
        "KICK_SCOOTER" to FitnessActivities.KICK_SCOOTER,
        "KICKBOXING" to FitnessActivities.KICKBOXING,
        "KITE_SURFING" to FitnessActivities.KITESURFING,
        "MARTIAL_ARTS" to FitnessActivities.MARTIAL_ARTS,
        "MEDITATION" to FitnessActivities.MEDITATION,
        "MIXED_MARTIAL_ARTS" to FitnessActivities.MIXED_MARTIAL_ARTS,
        "P90X" to FitnessActivities.P90X,
        "PARAGLIDING" to FitnessActivities.PARAGLIDING,
        "PILATES" to FitnessActivities.PILATES,
        "POLO" to FitnessActivities.POLO,
        "RACQUETBALL" to FitnessActivities.RACQUETBALL,
        "ROCK_CLIMBING" to FitnessActivities.ROCK_CLIMBING,
        "ROWING" to FitnessActivities.ROWING,
        "RUGBY" to FitnessActivities.RUGBY,
        "RUNNING_JOGGING" to FitnessActivities.RUNNING_JOGGING,
        "RUNNING_SAND" to FitnessActivities.RUNNING_SAND,
        "RUNNING_TREADMILL" to FitnessActivities.RUNNING_TREADMILL,
        "RUNNING" to FitnessActivities.RUNNING,
        "SAILING" to FitnessActivities.SAILING,
        "SCUBA_DIVING" to FitnessActivities.SCUBA_DIVING,
        "SKATING_CROSS" to FitnessActivities.SKATING_CROSS,
        "SKATING_INDOOR" to FitnessActivities.SKATING_INDOOR,
        "SKATING_INLINE" to FitnessActivities.SKATING_INLINE,
        "SKATING" to FitnessActivities.SKATING,
        "SKIING_BACK_COUNTRY" to FitnessActivities.SKIING_BACK_COUNTRY,
        "SKIING_KITE" to FitnessActivities.SKIING_KITE,
        "SKIING_ROLLER" to FitnessActivities.SKIING_ROLLER,
        "SLEDDING" to FitnessActivities.SLEDDING,
        "SNOWBOARDING" to FitnessActivities.SNOWBOARDING,
        "SOCCER" to FitnessActivities.FOOTBALL_SOCCER,
        "SOFTBALL" to FitnessActivities.SOFTBALL,
        "SQUASH" to FitnessActivities.SQUASH,
        "STAIR_CLIMBING_MACHINE" to FitnessActivities.STAIR_CLIMBING_MACHINE,
        "STAIR_CLIMBING" to FitnessActivities.STAIR_CLIMBING,
        "STANDUP_PADDLEBOARDING" to FitnessActivities.STANDUP_PADDLEBOARDING,
        "STILL" to FitnessActivities.STILL,
        "STRENGTH_TRAINING" to FitnessActivities.STRENGTH_TRAINING,
        "SURFING" to FitnessActivities.SURFING,
        "SWIMMING_OPEN_WATER" to FitnessActivities.SWIMMING_OPEN_WATER,
        "SWIMMING_POOL" to FitnessActivities.SWIMMING_POOL,
        "SWIMMING" to FitnessActivities.SWIMMING,
        "TABLE_TENNIS" to FitnessActivities.TABLE_TENNIS,
        "TEAM_SPORTS" to FitnessActivities.TEAM_SPORTS,
        "TENNIS" to FitnessActivities.TENNIS,
        "TILTING" to FitnessActivities.TILTING,
        "VOLLEYBALL_BEACH" to FitnessActivities.VOLLEYBALL_BEACH,
        "VOLLEYBALL_INDOOR" to FitnessActivities.VOLLEYBALL_INDOOR,
        "VOLLEYBALL" to FitnessActivities.VOLLEYBALL,
        "WAKEBOARDING" to FitnessActivities.WAKEBOARDING,
        "WALKING_FITNESS" to FitnessActivities.WALKING_FITNESS,
        "WALKING_NORDIC" to FitnessActivities.WALKING_NORDIC,
        "WALKING_STROLLER" to FitnessActivities.WALKING_STROLLER,
        "WALKING_TREADMILL" to FitnessActivities.WALKING_TREADMILL,
        "WALKING" to FitnessActivities.WALKING,
        "WATER_POLO" to FitnessActivities.WATER_POLO,
        "WEIGHTLIFTING" to FitnessActivities.WEIGHTLIFTING,
        "WHEELCHAIR" to FitnessActivities.WHEELCHAIR,
        "WINDSURFING" to FitnessActivities.WINDSURFING,
        "YOGA" to FitnessActivities.YOGA,
        "ZUMBA" to FitnessActivities.ZUMBA,
        "OTHER" to FitnessActivities.OTHER,
    )

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
        channel?.setMethodCallHandler(this)
        threadPoolExecutor = Executors.newFixedThreadPool(4)
        handler = Handler()
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        threadPoolExecutor?.shutdown()
        threadPoolExecutor = null
        handler = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityBinding = binding
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        activityBinding?.removeActivityResultListener(this)
        activityBinding = null
        activity = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("FLUTTER_HEALTH", "Access Granted!")
                mResult?.success(true)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("FLUTTER_HEALTH", "Access Denied!")
                mResult?.success(false)
            }
            return true
        }
        return false
    }

    private fun keyToHealthDataType(type: String): DataType {
        return when (type) {
            BODY_FAT_PERCENTAGE -> DataType.TYPE_BODY_FAT_PERCENTAGE
            HEIGHT -> DataType.TYPE_HEIGHT
            WEIGHT -> DataType.TYPE_WEIGHT
            STEPS -> DataType.TYPE_STEP_COUNT_DELTA
            AGGREGATE_STEP_COUNT -> DataType.AGGREGATE_STEP_COUNT_DELTA
            ACTIVE_ENERGY_BURNED -> DataType.TYPE_CALORIES_EXPENDED
            HEART_RATE -> DataType.TYPE_HEART_RATE_BPM
            BODY_TEMPERATURE -> HealthDataTypes.TYPE_BODY_TEMPERATURE
            BLOOD_PRESSURE_SYSTOLIC -> HealthDataTypes.TYPE_BLOOD_PRESSURE
            BLOOD_PRESSURE_DIASTOLIC -> HealthDataTypes.TYPE_BLOOD_PRESSURE
            BLOOD_OXYGEN -> HealthDataTypes.TYPE_OXYGEN_SATURATION
            BLOOD_GLUCOSE -> HealthDataTypes.TYPE_BLOOD_GLUCOSE
            MOVE_MINUTES -> DataType.TYPE_MOVE_MINUTES
            DISTANCE_DELTA -> DataType.TYPE_DISTANCE_DELTA
            WATER -> DataType.TYPE_HYDRATION
            SLEEP_ASLEEP -> DataType.TYPE_SLEEP_SEGMENT
            SLEEP_AWAKE -> DataType.TYPE_SLEEP_SEGMENT
            SLEEP_IN_BED -> DataType.TYPE_SLEEP_SEGMENT
            WORKOUT -> DataType.TYPE_ACTIVITY_SEGMENT
            else -> throw IllegalArgumentException("Unsupported dataType: $type")
        }
    }

    private fun getField(type: String): Field {
        return when (type) {
            BODY_FAT_PERCENTAGE -> Field.FIELD_PERCENTAGE
            HEIGHT -> Field.FIELD_HEIGHT
            WEIGHT -> Field.FIELD_WEIGHT
            STEPS -> Field.FIELD_STEPS
            ACTIVE_ENERGY_BURNED -> Field.FIELD_CALORIES
            HEART_RATE -> Field.FIELD_BPM
            BODY_TEMPERATURE -> HealthFields.FIELD_BODY_TEMPERATURE
            BLOOD_PRESSURE_SYSTOLIC -> HealthFields.FIELD_BLOOD_PRESSURE_SYSTOLIC
            BLOOD_PRESSURE_DIASTOLIC -> HealthFields.FIELD_BLOOD_PRESSURE_DIASTOLIC
            BLOOD_OXYGEN -> HealthFields.FIELD_OXYGEN_SATURATION
            BLOOD_GLUCOSE -> HealthFields.FIELD_BLOOD_GLUCOSE_LEVEL
            MOVE_MINUTES -> Field.FIELD_DURATION
            DISTANCE_DELTA -> Field.FIELD_DISTANCE
            WATER -> Field.FIELD_VOLUME
            SLEEP_ASLEEP -> Field.FIELD_SLEEP_SEGMENT_TYPE
            SLEEP_AWAKE -> Field.FIELD_SLEEP_SEGMENT_TYPE
            SLEEP_IN_BED -> Field.FIELD_SLEEP_SEGMENT_TYPE
            WORKOUT -> Field.FIELD_ACTIVITY
            else -> throw IllegalArgumentException("Unsupported dataType: $type")
        }
    }

    // ... (rest of your existing methods remain the same - writeData, getData, etc.)

    /// Handle calls from the MethodChannel
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "requestAuthorization" -> requestAuthorization(call, result)
            "getData" -> getData(call, result)
            "writeData" -> writeData(call, result)
            "getTotalStepsInInterval" -> getTotalStepsInInterval(call, result)
            "hasPermissions" -> hasPermissions(call, result)
            "writeWorkoutData" -> writeWorkoutData(call, result)
            else -> result.notImplemented()
        }
    }

    // ... (include all your existing methods: writeData, getData, requestAuthorization, etc.)
    // Just copy all the remaining methods from your original file - they should work as-is
    // The key changes are in the class declaration and the activity/plugin lifecycle methods

    private fun writeData(call: MethodCall, result: Result) {
        // Your existing writeData implementation
        if (activity == null) {
            result.success(false)
            return
        }

        val type = call.argument<String>("dataTypeKey")!!
        val startTime = call.argument<Long>("startTime")!!
        val endTime = call.argument<Long>("endTime")!!
        val value = call.argument<Float>("value")!!

        // Look up data type and unit for the type key
        val dataType = keyToHealthDataType(type)
        val field = getField(type)

        val typesBuilder = FitnessOptions.builder()
        typesBuilder.addDataType(dataType, FitnessOptions.ACCESS_WRITE)

        val dataSource = DataSource.Builder()
            .setDataType(dataType)
            .setType(DataSource.TYPE_RAW)
            .setDevice(Device.getLocalDevice(activity!!.applicationContext))
            .setAppPackageName(activity!!.applicationContext)
            .build()

        val builder = if (startTime == endTime)
            DataPoint.builder(dataSource)
                .setTimestamp(startTime, TimeUnit.MILLISECONDS)
        else
            DataPoint.builder(dataSource)
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)

        // Conversion is needed because glucose is stored as mmoll in Google Fit;
        // while mgdl is used for glucose in this plugin.
        val isGlucose = field == HealthFields.FIELD_BLOOD_GLUCOSE_LEVEL
        val dataPoint = if (!isIntField(dataSource, field))
            builder.setField(field, (if (!isGlucose) value else (value / MMOLL_2_MGDL).toFloat()))
                .build() else
            builder.setField(field, value.toInt()).build()

        val dataSet = DataSet.builder(dataSource)
            .add(dataPoint)
            .build()

        if (dataType == DataType.TYPE_SLEEP_SEGMENT) {
            typesBuilder.accessSleepSessions(FitnessOptions.ACCESS_READ)
        }
        val fitnessOptions = typesBuilder.build()
        try {
            val googleSignInAccount =
                GoogleSignIn.getAccountForExtension(activity!!.applicationContext, fitnessOptions)
            Fitness.getHistoryClient(activity!!.applicationContext, googleSignInAccount)
                .insertData(dataSet)
                .addOnSuccessListener {
                    Log.i("FLUTTER_HEALTH::SUCCESS", "DataSet added successfully!")
                    result.success(true)
                }
                .addOnFailureListener { e ->
                    Log.w("FLUTTER_HEALTH::ERROR", "There was an error adding the DataSet", e)
                    result.success(false)
                }
        } catch (e3: Exception) {
            result.success(false)
        }
    }

    private fun isIntField(dataSource: DataSource, unit: Field): Boolean {
        val dataPoint = DataPoint.builder(dataSource).build()
        val value = dataPoint.getValue(unit)
        return value.format == Field.FORMAT_INT32
    }

    private fun getHealthDataValue(dataPoint: DataPoint, field: Field): Any {
        val value = dataPoint.getValue(field)
        // Conversion is needed because glucose is stored as mmoll in Google Fit;
        // while mgdl is used for glucose in this plugin.
        val isGlucose = field == HealthFields.FIELD_BLOOD_GLUCOSE_LEVEL
        return when (value.format) {
            Field.FORMAT_FLOAT -> if (!isGlucose) value.asFloat() else value.asFloat() * MMOLL_2_MGDL
            Field.FORMAT_INT32 -> value.asInt()
            Field.FORMAT_STRING -> value.asString()
            else -> Log.e("Unsupported format:", value.format.toString())
        }
    }

    // ... (include all your other existing methods)
}

/**
 * Names for the {@code SleepStages} values.
 */
val SLEEP_STAGES = arrayOf(
    "Unused",
    "Awake (during sleep)",
    "Sleep",
    "Out-of-bed",
    "Light sleep",
    "Deep sleep",
    "REM sleep"
)