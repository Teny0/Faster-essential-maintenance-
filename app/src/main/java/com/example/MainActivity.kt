package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.MaintenanceRepository
import com.example.ui.MaintainHubApp
import com.example.ui.MaintenanceViewModel
import com.example.ui.MaintenanceViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local Room database and repository layers
        val database = AppDatabase.getDatabase(this)
        val repository = MaintenanceRepository(database.maintenanceDao())
        
        // Build ViewModel via custom constructor factory
        val viewModel: MaintenanceViewModel by viewModels {
            MaintenanceViewModelFactory(repository)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MaintainHubApp(viewModel = viewModel)
            }
        }
    }
}
