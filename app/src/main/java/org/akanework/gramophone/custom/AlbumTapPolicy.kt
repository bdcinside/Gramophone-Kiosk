// BEISPIELMODUL: Die App-eigenen Wiedergabe- und Navigationsfunktionen anbinden.
package org.akanework.gramophone.custom

/**
 * Ist bereits Musik hörbar, wird nur navigiert (ohne Queue-Änderung).
 * Ist keine Musik hörbar, startet der komplette Albumeintrag ab Track 1;
 * anschließend wird ebenfalls die Albumdetailseite geöffnet.
 * `isPlaying` muss aus der tatsächlichen Media3-Player-Session kommen.
 */
inline fun handleAlbumCoverTap(
    isPlaying: Boolean,
    startCompleteAlbumInTrackOrder: () -> Unit,
    openAlbumDetails: () -> Unit,
) {
    if (!isPlaying) startCompleteAlbumInTrackOrder()
    openAlbumDetails()
}
