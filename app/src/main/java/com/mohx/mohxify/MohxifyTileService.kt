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
        Rules.setEnabled(this, !Rules.enabled(this))
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let {
            val active = Rules.enabled(this)
            it.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.label = if (active) "Mohxify ON" else "Mohxify OFF"
            it.updateTile()
        }
    }
}
