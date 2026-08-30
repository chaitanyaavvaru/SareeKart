
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';

export default function SearchBar({ placeholder = "Search sarees..." }) {
  const [query, setQuery] = useState('');
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    if (query.trim()) {
      navigate(`/products?search=${encodeURIComponent(query.trim())}`);
      setQuery('');
    }
  };

  return (
    <form onSubmit={handleSearch} className="relative flex items-center bg-white/10 focus-within:bg-white border border-white/20 focus-within:border-gold text-white focus-within:text-text-primary rounded-full px-4 py-1.5 transition-all w-full max-w-[240px]">
      <input
        type="text"
        placeholder={placeholder}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        className="bg-transparent border-none outline-none text-xs w-full pr-6 placeholder-white/60 focus-within:placeholder-gray-400 font-sans"
      />
      <button type="submit" className="absolute right-3 p-0.5 hover:text-gold transition-colors focus:outline-none">
        <Search className="w-4 h-4 cursor-pointer" />
      </button>
    </form>
  );
}
