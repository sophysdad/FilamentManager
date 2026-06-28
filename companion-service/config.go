package main

import (
	"encoding/json"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"time"
)

type SlicerConfig struct {
	ID      string `json:"id"`
	Name    string `json:"name"`
	Folder  string `json:"folder"`
	Enabled bool   `json:"enabled"`
}

type Config struct {
	Slicers []SlicerConfig `json:"slicers"`
}

func (c *Config) enabledSlicers() []SlicerConfig {
	var out []SlicerConfig
	for _, s := range c.Slicers {
		if s.Enabled {
			out = append(out, s)
		}
	}
	return out
}

// primaryFolder returns the folder of the first enabled slicer, falling back to the
// platform default if nothing is configured.
func (c *Config) primaryFolder() string {
	for _, s := range c.Slicers {
		if s.Enabled {
			return s.Folder
		}
	}
	return defaultProfileFolder()
}

func (c *Config) slicerByID(id string) *SlicerConfig {
	for i := range c.Slicers {
		if c.Slicers[i].ID == id {
			return &c.Slicers[i]
		}
	}
	return nil
}

func appConfigDir() string {
	switch runtime.GOOS {
	case "windows":
		return filepath.Join(os.Getenv("APPDATA"), "FilamentProfileManager")
	case "darwin":
		home, _ := os.UserHomeDir()
		return filepath.Join(home, "Library", "Application Support", "FilamentProfileManager")
	default:
		home, _ := os.UserHomeDir()
		return filepath.Join(home, ".config", "filament-manager")
	}
}

func configFilePath() string {
	return filepath.Join(appConfigDir(), "config.json")
}

func loadConfig(legacyFolder string) (*Config, error) {
	path := configFilePath()
	data, err := os.ReadFile(path)
	if os.IsNotExist(err) {
		cfg := &Config{Slicers: autoDetectSlicers()}
		// If -folder was explicitly set and differs from all detected slicers, prepend it.
		if legacyFolder != "" {
			found := false
			for _, s := range cfg.Slicers {
				if s.Folder == legacyFolder {
					found = true
					break
				}
			}
			if !found {
				cfg.Slicers = append([]SlicerConfig{{
					ID: "custom", Name: "Custom", Folder: legacyFolder, Enabled: true,
				}}, cfg.Slicers...)
			}
		}
		_ = saveConfig(cfg)
		return cfg, nil
	}
	if err != nil {
		return nil, err
	}
	var cfg Config
	if err := json.Unmarshal(data, &cfg); err != nil {
		return nil, err
	}

	// Update existing slicers with a better (e.g. numeric user) folder if auto-detect finds one,
	// or add if completely new. This way we use the correct user folder while keeping one entry
	// per slicer name, and system profiles continue to be auto-derived from the slicer root.
	discovered := autoDetectSlicers()
	existingByID := map[string]int{}
	for i := range cfg.Slicers {
		existingByID[cfg.Slicers[i].ID] = i
	}
	for _, d := range discovered {
		if idx, ok := existingByID[d.ID]; ok {
			if cfg.Slicers[idx].Folder != d.Folder {
				cfg.Slicers[idx].Folder = d.Folder
			}
		} else {
			// fallback for old configs that used "anycubic-xxx" style IDs
			base := strings.Split(d.ID, "-")[0]
			found := false
			for id, idx2 := range existingByID {
				if strings.HasPrefix(id, base) || strings.HasPrefix(base, strings.Split(id, "-")[0]) {
					if cfg.Slicers[idx2].Folder != d.Folder {
						cfg.Slicers[idx2].Folder = d.Folder
					}
					found = true
					break
				}
			}
			if !found {
				cfg.Slicers = append(cfg.Slicers, d)
				existingByID[d.ID] = len(cfg.Slicers) - 1
			}
		}
	}

	// Re-sort so that more recently modified filament folders float to the top.
	// This makes primaryFolder() (used by the app and systray) prefer the active user folder.
	sort.SliceStable(cfg.Slicers, func(i, j int) bool {
		ti, _ := os.Stat(cfg.Slicers[i].Folder)
		tj, _ := os.Stat(cfg.Slicers[j].Folder)
		if ti != nil && tj != nil {
			return ti.ModTime().After(tj.ModTime())
		}
		return i < j
	})

	// If after reordering the first item is disabled but there is a later enabled one for the same family, swap to make a good primary.
	// (Keeps the most recent enabled as early as possible.)
	if len(cfg.Slicers) > 0 && !cfg.Slicers[0].Enabled {
		for i := 1; i < len(cfg.Slicers); i++ {
			if cfg.Slicers[i].Enabled {
				// simple swap with first
				cfg.Slicers[0], cfg.Slicers[i] = cfg.Slicers[i], cfg.Slicers[0]
				break
			}
		}
	}

	// Persist any auto-chosen better user folder so it sticks across restarts
	// (user can still override via the settings page).
	_ = saveConfig(&cfg)

	return &cfg, nil
}

func saveConfig(cfg *Config) error {
	if err := os.MkdirAll(appConfigDir(), 0755); err != nil {
		return err
	}
	data, err := json.MarshalIndent(cfg, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(configFilePath(), data, 0644)
}

// knownForks lists OrcaSlicer-compatible slicer forks and their standard filament subfolder
// relative to the platform AppData/config root. Auto-detection prefers a non-default
// (numeric) user folder when present for the active user's profiles; system profiles
// are always derived automatically from the slicer root. The user folder can be
// manually overridden per slicer in the settings UI.
var knownForks = []struct{ ID, Name, RelPath string }{
	{"orca", "OrcaSlicer", "OrcaSlicer/user/default/filament"},
	{"bambu", "Bambu Studio", "BambuStudio/user/default/filament"},
	{"elegoo", "Elegoo Slicer", "ElegooSlicer/user/default/filament"},
	{"anycubic", "Anycubic Slicer", "AnycubicSlicerNext/user/default/filament"},
	{"anycubic2", "Anycubic Slicer", "AnycubicSlicer/user/default/filament"},
	{"anycubic3", "Anycubic Slicer", "Anycubic Slicer/user/default/filament"},
	{"flashstudio", "Flash Studio", "FlashStudio/user/default/filament"},
	{"flashstudio2", "Flash Studio", "Flash Studio/user/default/filament"},
	{"flashforge", "Orca-Flashforge", "OrcaFlashforge/user/default/filament"},
	{"creality", "Creality Print", "CrealityPrint/user/default/filament"},
}

func autoDetectSlicers() []SlicerConfig {
	var base string
	switch runtime.GOOS {
	case "windows":
		base = os.Getenv("APPDATA")
	case "darwin":
		home, _ := os.UserHomeDir()
		base = filepath.Join(home, "Library", "Application Support")
	default:
		home, _ := os.UserHomeDir()
		base = filepath.Join(home, ".config")
	}

	var result []SlicerConfig
	seen := make(map[string]bool) // dedup by canonical ID

	for _, f := range knownForks {
		canonicalID := strings.TrimRight(f.ID, "0123456789")
		if seen[canonicalID] {
			continue
		}

		slicerBase := filepath.Join(base, filepath.FromSlash(strings.TrimSuffix(f.RelPath, "/user/default/filament")))
		userDir := filepath.Join(slicerBase, "user")

		var candidates []string

		// default
		def := filepath.Join(userDir, "default", "filament")
		if _, err := os.Stat(def); err == nil {
			candidates = append(candidates, def)
		}

		// any other user subfolders (numeric etc.)
		if entries, err := os.ReadDir(userDir); err == nil {
			for _, e := range entries {
				if !e.IsDir() || e.Name() == "default" {
					continue
				}
				cand := filepath.Join(userDir, e.Name(), "filament")
				if _, err := os.Stat(cand); err == nil {
					candidates = append(candidates, cand)
				}
			}
		}

		if len(candidates) == 0 {
			continue
		}

		// Pick the "best": the most recently modified non-default folder if one exists,
		// otherwise the most recent default.
		var best string
		var bestTime time.Time
		hasNonDefault := false
		for _, c := range candidates {
			isDef := strings.Contains(c, "/default/") || strings.Contains(c, `\default\`)
			st, err := os.Stat(c)
			if err != nil {
				continue
			}
			t := st.ModTime()
			if !isDef {
				if !hasNonDefault || t.After(bestTime) {
					best = c
					bestTime = t
					hasNonDefault = true
				}
			} else if !hasNonDefault && (best == "" || t.After(bestTime)) {
				best = c
				bestTime = t
			}
		}

		if best == "" {
			continue
		}

		seen[canonicalID] = true
		result = append(result, SlicerConfig{
			ID:      canonicalID,
			Name:    f.Name,
			Folder:  best,
			Enabled: true,
		})
	}

	// Sort by most recent for better primaryFolder ordering
	sort.SliceStable(result, func(i, j int) bool {
		ti, _ := os.Stat(result[i].Folder)
		tj, _ := os.Stat(result[j].Folder)
		if ti != nil && tj != nil {
			return ti.ModTime().After(tj.ModTime())
		}
		return i < j
	})

	return result
}