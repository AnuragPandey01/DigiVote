package xyz.droidev.eventsync

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import xyz.droidev.eventsync.data.api.ApiService
import xyz.droidev.eventsync.data.api.FaceMeshPoint
import xyz.droidev.eventsync.data.api.UserData
import xyz.droidev.eventsync.databinding.FragmentFirstBinding
import xyz.droidev.eventsync.util.FileProvider
import javax.inject.Inject


@AndroidEntryPoint
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyViewModel by viewModels()
//    private val textToSpeech : TextToSpeech by lazy {
//        TextToSpeech(requireContext()){ }
//    }
    private val defaultDetector = FaceMeshDetection.getClient()
    private val progressDialog by lazy { ProgressDialog(requireContext()) }

    private val imageUri by lazy{ FileProvider.getImageUri(requireContext()) }
    @Inject
    lateinit var apiService: ApiService


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        textToSpeech.setVoice(Voice("vipul",Locale("hi","IN"),300,300,true,null))
//        textToSpeech.setLanguage(Locale("hi", "IN"))
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        val launcher = registerForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ){ result ->
//            if(result.resultCode == RESULT_OK){
//                Toast.makeText(requireContext(),"result ok",Toast.LENGTH_SHORT).show();
//                result.data?.let{
//                    val resTxt = it.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
//                    binding.text.text= resTxt[0]
//                }
//            }
//        }
//        binding.audioButton.setOnClickListener {
//            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//            intent.putExtra(
//                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
//            )
//            intent.putExtra(
//                RecognizerIntent.EXTRA_LANGUAGE,
//                Locale.getDefault()
//            )
//            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
//            try {
//                launcher.launch(intent)
//            } catch (e: Exception) {
//                Toast.makeText(requireContext(),"error",Toast.LENGTH_SHORT).show();
//            }
//            //textToSpeech.speak("Dhruv sane",TextToSpeech.QUEUE_ADD,null,"u")
//        }

        registerListeners()
    }

    private fun registerListeners() {
        val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if(it){
                binding.imagePreviewLayout.visibility = View.VISIBLE
                binding.clickImageButton.visibility = View.GONE
                binding.imagePreview.setImageURI(imageUri)
            }
        }

        binding.closeImageButton.setOnClickListener {
            binding.imagePreviewLayout.visibility = View.GONE
            binding.clickImageButton.visibility = View.VISIBLE
            binding.imagePreview.setImageURI(null)
        }

        binding.clickImageButton.setOnClickListener {
            cameraLauncher.launch(imageUri)
        }

        binding.generateButton.setOnClickListener {
            progressDialog.show()
            val aadhar = binding.aadharInput.text.toString()
            val name = binding.nameInput.text.toString()
            val mob = binding.phoneInput.text.toString()
            lifecycleScope.launch {
                try{
                    val imageInput = InputImage.fromFilePath(requireContext(),imageUri)
                    val result = defaultDetector.process(imageInput).await()
                    if(result.size > 1) throw MultiFaceException()
                    if(result.size == 0) throw NoFaceException()

                    val res = apiService.postUserData(
                        UserData(
                            aadhaar = aadhar,
                            full_name = name,
                            phone = mob,
                            bounds = listOf(
                                result[0].boundingBox.left.toFloat(),
                                result[0].boundingBox.right.toFloat(),
                                result[0].boundingBox.top.toFloat(),
                                result[0].boundingBox.bottom.toFloat()
                            ),
                            face_points = result[0].allPoints.map{ FaceMeshPoint(it.index,
                                listOf(it.position.x,it.position.y)
                            ) },
                            triangle_indices = listOf(listOf(0,1,2))
                        )
                    )
                    findNavController().navigate(FirstFragmentDirections.actionFirstFragmentToSecondFragment(res.id))
                }catch (e: MultiFaceException){
                    Toast.makeText(
                        requireContext(),
                        "No face detected",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: MultiFaceException){
                    Toast.makeText(
                        requireContext(),
                        "Two or more faces detected",
                        Toast.LENGTH_SHORT
                    ).show()
                }catch (e:Exception){
                    Toast.makeText(requireContext(), "some error occurred", Toast.LENGTH_SHORT).show()
                }finally {
                    progressDialog.dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MultiFaceException: Exception()

class NoFaceException: Exception()

@HiltViewModel
class MyViewModel @Inject constructor(
) : ViewModel() {
    // ViewModel code
    val data = "this is data"
}