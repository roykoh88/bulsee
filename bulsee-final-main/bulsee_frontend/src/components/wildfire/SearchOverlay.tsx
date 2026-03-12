// src/components/wildfire/SearchOverlay.tsx
import { useState, useEffect, useRef } from "react";
import { Search, X, MapPin } from "lucide-react";
import type { RegionData } from "../../types/wildfire";

interface SearchOverlayProps {
  regions: RegionData[];
  onSelectRegion: (region: RegionData) => void;
}

export function SearchOverlay({ regions, onSelectRegion }: SearchOverlayProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [suggestions, setSuggestions] = useState<RegionData[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (searchTerm.trim() === "") {
      setSuggestions([]);
      setIsOpen(false);
      return;
    }

    const matched = regions.filter(
      (r) => r.name.includes(searchTerm) || (r.location && r.location.includes(searchTerm))
    );

    setSuggestions(matched);
    setIsOpen(true);
  }, [searchTerm, regions]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (region: RegionData) => {
    onSelectRegion(region);
    setSearchTerm(region.name);
    setIsOpen(false);
  };

  const handleClear = () => {
    setSearchTerm("");
    setSuggestions([]);
    setIsOpen(false);
  };

  return (
    <div ref={wrapperRef} className="absolute top-4 left-4 z-[50] w-[360px]">
      <div className="relative group">
        <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
          <Search size={18} className="text-gray-400 group-focus-within:text-blue-500 transition-colors" />
        </div>
        <input
          type="text"
          className="w-full pl-10 pr-10 py-3.5 bg-white/95 backdrop-blur-md border border-white/20 rounded-2xl shadow-lg text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all font-medium"
          placeholder="지역명 검색 (예: 춘천)"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onFocus={() => { if (searchTerm) setIsOpen(true); }}
        />
        {searchTerm && (
          <button
            onClick={handleClear}
            className="absolute inset-y-0 right-3 flex items-center text-gray-400 hover:text-gray-600 cursor-pointer"
          >
            <X size={18} />
          </button>
        )}
      </div>

      {isOpen && suggestions.length > 0 && (
        <ul className="absolute top-full left-0 right-0 mt-2 bg-white/95 backdrop-blur-md rounded-2xl shadow-xl border border-gray-100 max-h-[400px] overflow-y-auto custom-scrollbar overflow-hidden">
          {suggestions.map((region) => (
            <li
              key={region.sigCd}
              onClick={() => handleSelect(region)}
              className="px-5 py-3.5 hover:bg-blue-50 cursor-pointer flex items-center gap-3 transition-colors border-b last:border-none border-gray-50"
            >
              <div className="p-2 bg-gray-100 rounded-full text-gray-500">
                <MapPin size={16} />
              </div>
              <div>
                <p className="text-sm font-bold text-gray-900">{region.name}</p>
                <p className="text-xs text-gray-400 mt-0.5">
                  SIG: {region.sigCd} · STN: {region.stnId}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
