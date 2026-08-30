export default function Shimmer({ type = "text", count = 1 }) {
  if (type === "card") {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        {[...Array(count)].map((_, idx) => (
          <div key={idx} className="bg-white border border-[#F4F4F4] rounded-2xl overflow-hidden p-4 space-y-4 animate-pulse">
            <div className="h-80 bg-gray-200 rounded-xl" />
            <div className="space-y-2">
              <div className="h-3 bg-gray-200 rounded w-1/4" />
              <div className="h-4 bg-gray-200 rounded w-3/4" />
              <div className="h-3 bg-gray-200 rounded w-1/2" />
            </div>
            <div className="flex gap-2 pt-2">
              <div className="h-9 bg-gray-200 rounded flex-1" />
              <div className="h-9 bg-gray-200 rounded flex-1" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  // Fallback text shimmer lines
  return (
    <div className="space-y-3">
      {[...Array(count)].map((_, idx) => (
        <div key={idx} className="h-4 bg-gray-200 rounded animate-pulse w-full" />
      ))}
    </div>
  );
}
