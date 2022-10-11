package com.popsa.interview.dragtoswap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Place for view data
 */
class MainActivityViewModel : ViewModel() {

    val images = MutableLiveData<List<Image>>()
    val events = SingleLiveEvent<MainActivityCoordinator.Events>()
    val draggingIndex = MutableLiveData<Int?>()
}