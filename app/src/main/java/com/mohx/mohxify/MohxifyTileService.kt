package com.mohx.mohxify

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class MohxifyTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        LoadoutStore.setEnabled(this, !LoadoutStore.enabled(this))
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let {
            val active = LoadoutStore.enabled(this)
            val name = LoadoutStore.active(this).name
            it.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.label = name.take(20)
            it.contentDescription = "Mohxify ${if (active) "ON" else "OFF"} · $name"
            it.updateTile()
        }
    }
}
