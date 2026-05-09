package com.gustavo.brilhante.cutestickers.ui

import androidx.lifecycle.ViewModel
import com.gustavo.brilhante.cutestickers.common.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel()
