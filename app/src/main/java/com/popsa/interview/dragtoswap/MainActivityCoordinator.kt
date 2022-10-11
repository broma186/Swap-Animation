package com.popsa.interview.dragtoswap

import android.view.View

/**
 * Place for logic
 */
class MainActivityCoordinator(
    private val viewModel: MainActivityViewModel
) {

    private val imageRepository = FakeDi.imageRepository

    init {
        viewModel.images.value = imageRepository.list
    }

    fun swapImages(position1: Int, position2: Int) {
        val item1 = imageRepository.list[position1]
        val item2 = imageRepository.list[position2]
        imageRepository.list[position1] = item2
        imageRepository.list[position2] = item1
        viewModel.images.value = imageRepository.list
        cancelSwap()
    }

    fun startedSwap(index: Int) {
        viewModel.draggingIndex.value = index
        viewModel.events.value = Events.ImageSelected
    }

    fun cancelSwap() {
        viewModel.draggingIndex.value = null
    }

    fun imageDropped(view: View, eventX: Int, eventY: Int) {
        viewModel.events.value = Events.ImageDropped(view, eventX, eventY)
    }

    sealed class Events {
        data class ImageDropped(val view: View, val x: Int, val y: Int) : Events()
        object ImageSelected: Events()
    }
}