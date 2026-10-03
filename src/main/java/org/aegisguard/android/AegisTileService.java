package org.aegisguard.android;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Android Quick Settings Notification Shade Tile.
 * Enables 1-click toggling of systemwide ad blocking without having to open the app.
 */
public class AegisTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean active = AegisVpnService.isRunning.get();

        if (active) {
            Intent stopIntent = new Intent(this, AegisVpnService.class);
            stopIntent.setAction(AegisVpnService.ACTION_STOP);
            startService(stopIntent);
        } else {
            Intent startIntent = new Intent(this, AegisVpnService.class);
            startIntent.setAction(AegisVpnService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent);
            } else {
                startService(startIntent);
            }
        }

        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean active = AegisVpnService.isRunning.get();
        tile.setLabel(getString(R.string.tile_name));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tile.setIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stat_shield));
        }

        if (active) {
            tile.setState(Tile.STATE_ACTIVE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle("Protected");
            }
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle("Paused");
            }
        }
        tile.updateTile();
    }
}
