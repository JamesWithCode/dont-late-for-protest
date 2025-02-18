package com.team8.dlfl.repository

import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.FirebaseException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.team8.dlfl.dto.CommonResponseDto
import com.team8.dlfl.model.LoginUserModel
import com.team8.dlfl.model.RegisterUserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "AuthRepository"

class AuthRepository {

    private val auth = Firebase.auth
    private val database: FirebaseDatabase = Firebase.database
    private val reference  = database.getReference("user")
    private val uid=auth.currentUser?.uid

    fun logout() {
        auth.signOut()
    }

    fun observeInfo(_email: MutableLiveData<String>, _name: MutableLiveData<String>, _phone: MutableLiveData<String>) {
        reference.child(uid!!).child("email").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                _email.postValue(snapshot.value.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        reference.child(uid!!).child("name").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                _name.postValue(snapshot.value.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        reference.child(uid!!).child("phone").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                _phone.postValue(snapshot.value.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun postName(newName: String) {
        reference.child(uid!!).child("name").setValue(newName)
    }

    fun postPhone(newPhone: String) {
        reference.child(uid!!).child("phone").setValue(newPhone)
    }

    suspend fun createAccount(newUser:RegisterUserModel)= suspendCoroutine{

        Log.d(TAG, "파이어베이스 회원가입 진입")
        Log.d(TAG,"회원가입 동기로기다리기 시작")
        var result: CommonResponseDto
        var resumed=false
        CoroutineScope(Dispatchers.IO).launch {

            runBlocking {
                try {
                    auth.createUserWithEmailAndPassword(newUser.email, newUser.password)
                        .addOnCompleteListener { task ->
                            try {
                                Log.d(TAG, "회원가입 result: ${task.result}")
                                if (task.isSuccessful) {
                                    Log.d(TAG, "회원가입 성공")

                                    val uid = task.result.user?.uid
                                    Log.d(TAG,"uid: $uid")
                                    uid?.let {
                                        newUser.uid = uid
                                        reference.child(it).setValue(newUser)
                                    }
                                    Log.d(TAG, "result: ${task.isSuccessful}")
                                    result = CommonResponseDto(true,"회원가입 성공")
                                    Log.d(TAG,"회원가입 동기로기다리기 완료")
                                    if(!resumed){
                                        it.resume(result)
                                        resumed=true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "안 회원가입 실패: ${e.message}")
                                result = CommonResponseDto(false,"${e.message}")
                            }
                        }.await()
                } catch (e: FirebaseException) {
                    Log.d(TAG, "바깥 회원가입 실패: ${e.message}")
                    result = CommonResponseDto(false,"${e.message}")
                    it.resume(result)
                }
            }
        }

    }

    suspend fun loginAccount(loginUser:LoginUserModel)= suspendCoroutine{
        Log.d(TAG, "파이어베이스 로그인 진입")
        Log.d(TAG,"로그인 동기로기다리기 시작")
        var result=CommonResponseDto(false,"")
        var resumed=false
        CoroutineScope(Dispatchers.IO).launch {

            runBlocking {
                try{
                    auth.signInWithEmailAndPassword(loginUser.email, loginUser.password)
                        .addOnCompleteListener{ task ->

                            try {
                                Log.d(TAG, "로그인 result: ${task.result}")
                                if (task.isSuccessful) {
                                    Log.d(TAG, "로그인 성공")
                                    result = CommonResponseDto(true, "로그인 성공")
                                    it.resume(result)
                                }
                                result = CommonResponseDto(false,"${task.result}")

                                if(!resumed){
                                    it.resume(result)
                                    resumed=true
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "로그인 실패: ${e.message}")
                                result = CommonResponseDto(false,"${e.message}")
                            }
                        }.await()
                }catch(e:FirebaseException){
                    Log.d(TAG, "로그인 실패: ${e.message}")
                    result = CommonResponseDto(false,"${e.message}")
                    it.resume(result)
                }
            }
        }
    }



}