package com.gi.ch15_service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gi.ch15_outer.MyAIDLInterface
import com.gi.ch15_service.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    var connectionMode = "none"

    //aidl...........
    var aidlService: MyAIDLInterface? = null
    var aidlJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //aidl................
        onCreateAIDLService()

        // 권한 요청과 결과 처리를 위한 콜백 등록
        // 안드로이드 13부터 알림 표시하려면 POST_NOTIFICATIONS 권한 필요하다.
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (it.all { permission -> permission.value == true }) {
                onCreateJobScheduler()
            } else {
                Toast.makeText(this, "permission denied...", Toast.LENGTH_SHORT).show()
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onCreateJobScheduler()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        "android.permission.POST_NOTIFICATIONS"
                    )
                )
            }
        }else {
            onCreateJobScheduler()
        }
    }

    override fun onStop() {
        super.onStop()
        if(connectionMode === "aidl"){
            onStopAIDLService()
        }
        connectionMode="none"
        changeViewEnable()
    }

    fun changeViewEnable() = when (connectionMode) {
        "aidl" -> {
            binding.aidlPlay.isEnabled = false
            binding.aidlStop.isEnabled = true
        }
        else -> {
            //초기상태. stop 상태. 두 play 버튼 활성상태
            binding.aidlPlay.isEnabled = true
            binding.aidlStop.isEnabled = false
            binding.aidlProgress.progress = 0
        }
    }

    // 바인딩 서비스
    // AIDL 서비스에 바인딩 되었을 때 처리를 정의한 ServiceConection 콜백
    val aidlConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // 1. AIDL 인터페이스 객체 생성
            aidlService = MyAIDLInterface.Stub.asInterface(service)
            // 2. 음악 재생 시작
            aidlService!!.start()
            // 3. 프로그레스바 최대값 설정 (음악 총 재생시간)
            binding.aidlProgress.max = aidlService!!.maxDuration
            // 4. 백그라운드 스레드에서 프로그레스바 업데이트 시작
            val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
            aidlJob = backgroundScope.launch {
                while(binding.aidlProgress.progress < binding.aidlProgress.max){
                    delay(1000)
                    binding.aidlProgress.incrementProgressBy(1000)
                }
            }
            connectionMode = "aidl"
            changeViewEnable()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            aidlService = null
        }
    }

    // AIDL 통신 기법 사용, AIDL 에서는 bindService가 사용된다.
    // 재생 눌렀을 때 bindService 사용하여 외부 패키지(outer) 사용
    private fun onCreateAIDLService() {
        binding.aidlPlay.setOnClickListener {
            val intent = Intent("ACTION_SERVICE_AIDL")
            intent.setPackage("com.gi.ch15_outer")
            bindService(intent, aidlConnection, Context.BIND_AUTO_CREATE)
        }
        binding.aidlStop.setOnClickListener {
            aidlService!!.stop()
            unbindService(aidlConnection)
            aidlJob?.cancel()
            connectionMode = "none"
            changeViewEnable()
        }
    }
    private fun onStopAIDLService() {
        unbindService(aidlConnection)
    }

    //JobScheduler
    private fun onCreateJobScheduler(){
        var jobScheduler: JobScheduler? = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val builder = JobInfo.Builder(1, ComponentName(this, MyJobService::class.java))
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        val jobInfo = builder.build()
        jobScheduler!!.schedule(jobInfo)
    }

}