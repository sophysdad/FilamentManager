package main

import (
	"net/http"
	"os"
	"path/filepath"
	"runtime"
)

const serviceVersion = "0.2.0"
const defaultPort = 7878

func defaultProfileFolder() string {
	switch runtime.GOOS {
	case "windows":
		return filepath.Join(os.Getenv("APPDATA"), "OrcaSlicer", "user", "default", "filament")
	case "darwin":
		home, _ := os.UserHomeDir()
		return filepath.Join(home, "Library", "Application Support", "OrcaSlicer", "user", "default", "filament")
	default:
		home, _ := os.UserHomeDir()
		return filepath.Join(home, ".config", "OrcaSlicer", "user", "default", "filament")
	}
}

func newMux(cfgPath string, cfg *Config, port int) *http.ServeMux {
	h := newHandler(cfgPath, cfg, port)
	mux := http.NewServeMux()

	// Settings UI
	mux.HandleFunc("GET /settings", h.getSettings)
	mux.HandleFunc("POST /api/v1/settings", h.postSettings)
	mux.HandleFunc("GET /api/v1/browse-folder", h.browseFolder)

	// System profiles
	mux.HandleFunc("GET /api/v1/system-profiles", h.getSystemProfiles)
	mux.HandleFunc("GET /api/v1/system-profiles/{slicerID}/{name}", h.getSystemProfile)
	mux.HandleFunc("POST /api/v1/system-profiles/copy", h.copySystemProfile)

	// Multi-slicer
	mux.HandleFunc("GET /api/v1/slicers", h.getSlicers)
	mux.HandleFunc("POST /api/v1/profiles/{name}/sync", h.syncProfile)
	mux.HandleFunc("DELETE /api/v1/profiles/{name}/sync/{slicerID}", h.removeSyncProfile)

	// Existing profile routes
	mux.HandleFunc("GET /api/v1/status", h.getStatus)
	mux.HandleFunc("GET /api/v1/printers", h.getPrinters)
	mux.HandleFunc("GET /api/v1/profiles", h.listProfiles)
	mux.HandleFunc("GET /api/v1/profiles/{name}", h.getProfile)
	mux.HandleFunc("GET /api/v1/profiles/{name}/export", h.exportProfile)
	mux.HandleFunc("PUT /api/v1/profiles/{name}", h.updateProfile)
	mux.HandleFunc("POST /api/v1/profiles", h.createProfile)
	mux.HandleFunc("POST /api/v1/export/batch", h.exportBatch)
	mux.HandleFunc("DELETE /api/v1/profiles/{name}", h.deleteProfile)
	mux.HandleFunc("OPTIONS /", func(w http.ResponseWriter, r *http.Request) {
		setCORS(w)
		w.WriteHeader(http.StatusNoContent)
	})
	return mux
}

func corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		setCORS(w)
		next.ServeHTTP(w, r)
	})
}

func setCORS(w http.ResponseWriter) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
}