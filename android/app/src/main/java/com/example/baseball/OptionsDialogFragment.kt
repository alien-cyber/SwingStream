package com.example.baseball



import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.app.Dialog
import android.widget.TextView





import android.view.Gravity


class OptionsDialogFragment : DialogFragment() {

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            // Set the layout width and height
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val dialogWidth = (screenWidth * 0.3).toInt()
            val dialogHeight = (screenHeight * 0.9).toInt()

            dialog?.window?.setLayout(dialogWidth, dialogHeight)

            setGravity(Gravity.END)
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }

        isCancelable = false


//        // Dismiss dialog after a delay
//        Handler(Looper.getMainLooper()).postDelayed({
//            dismiss()
//        }, 3000) // 3 seconds
    }





    companion object {
        private const val ARG_OPTIONS = "options"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_NEXT_EVENT = "next_event"

        // Factory method to create an instance with data
        fun newInstance(options: List<String>, description: String, nextEvent: String): OptionsDialogFragment {
            val fragment = OptionsDialogFragment()
            val bundle = Bundle()

            // Add the list and additional strings to the bundle
            bundle.putStringArrayList(ARG_OPTIONS, ArrayList(options))
            bundle.putString(ARG_DESCRIPTION, description)
            bundle.putString(ARG_NEXT_EVENT, nextEvent)

            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.dialog_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val options = arguments?.getStringArrayList(ARG_OPTIONS) ?: listOf()
        val description = arguments?.getString(ARG_DESCRIPTION) ?: ""
        val nextEvent = arguments?.getString(ARG_NEXT_EVENT) ?: ""

        val container = view.findViewById<LinearLayout>(R.id.options_container)


        val descriptionTextView: TextView = view.findViewById(R.id.description_text)
        descriptionTextView.text = description

        val homeRunTextView: TextView = view.findViewById(R.id.nextevent)
        homeRunTextView.text = "$nextEvent predicted\n your prediction"


        // Create a button for each option
        options.forEach { option ->
            val button = Button(requireContext()).apply {
                text = option
                setOnClickListener {
                    sendDataToBackend(option)
                    dismiss()
                }
                setBackgroundResource(R.drawable.button_style) // Use custom style

                // Add margin to each button
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16 // Adjust the margin as needed
                }
                layoutParams = params
            }
            container.addView(button)
        }

    }

    private fun sendDataToBackend(selectedOption: String) {
        // Send the selected option to the backend
        Log.d("OptionsDialog", "Selected option: $selectedOption")
    }
}
