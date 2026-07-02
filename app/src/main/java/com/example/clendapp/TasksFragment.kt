package com.example.clendapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.example.clendapp.databinding.FragmentTasksBinding

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAllTaskMenus(binding.root)
    }

    private fun setupAllTaskMenus(root: View) {
        val buttons = mutableListOf<View>()
        findAllViewsWithTag(root, "task_options_button", buttons)
        for (button in buttons) {
            button.setOnClickListener { v ->
                showPopupMenu(v)
            }
        }
    }

    private fun findAllViewsWithTag(root: View, tag: String, result: MutableList<View>) {
        if (root.tag == tag) {
            result.add(root)
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findAllViewsWithTag(root.getChildAt(i), tag, result)
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Edit")
        popup.menu.add("Delete")
        popup.menu.add("Mark as done")
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
