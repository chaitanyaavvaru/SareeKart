import { Heart, ThumbsUp, HelpCircle } from 'lucide-react';

export default function VoteTally({ voteCounts = {}, item = null, onVote = null, userVote = null, disabled = false }) {
  const loveCount = (voteCounts && voteCounts.LOVE !== undefined) ? voteCounts.LOVE : (item?.loveCount || 0);
  const likeCount = (voteCounts && voteCounts.LIKE !== undefined) ? voteCounts.LIKE : (item?.likeCount || 0);
  const passCount = (voteCounts && (voteCounts.PASS !== undefined || voteCounts.SKIP !== undefined))
    ? (voteCounts.PASS || voteCounts.SKIP || 0)
    : (item?.passCount || 0);

  const handleVote = (reaction) => {
    if (disabled || !onVote) return;
    onVote(reaction);
  };

  return (
    <div className="flex items-center gap-1.5 sm:gap-2">
      <button
        type="button"
        disabled={disabled || !onVote}
        onClick={() => handleVote('LOVE')}
        title="Love this look"
        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold transition-all ${
          userVote === 'LOVE'
            ? 'bg-rose-500 text-white shadow-sm ring-2 ring-rose-300'
            : 'bg-rose-50 text-rose-700 hover:bg-rose-100'
        } ${onVote && !disabled ? 'cursor-pointer' : 'cursor-default'}`}
      >
        <Heart className={`w-3.5 h-3.5 ${userVote === 'LOVE' ? 'fill-current text-white' : 'fill-rose-500 text-rose-500'}`} />
        <span>{loveCount}</span>
      </button>

      <button
        type="button"
        disabled={disabled || !onVote}
        onClick={() => handleVote('LIKE')}
        title="Like this choice"
        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold transition-all ${
          userVote === 'LIKE'
            ? 'bg-emerald-600 text-white shadow-sm ring-2 ring-emerald-300'
            : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
        } ${onVote && !disabled ? 'cursor-pointer' : 'cursor-default'}`}
      >
        <ThumbsUp className={`w-3.5 h-3.5 ${userVote === 'LIKE' ? 'fill-current text-white' : 'text-emerald-600'}`} />
        <span>{likeCount}</span>
      </button>

      <button
        type="button"
        disabled={disabled || !onVote}
        onClick={() => handleVote('PASS')}
        title="Suggest alternative"
        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold transition-all ${
          userVote === 'PASS'
            ? 'bg-amber-600 text-white shadow-sm ring-2 ring-amber-300'
            : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
        } ${onVote && !disabled ? 'cursor-pointer' : 'cursor-default'}`}
      >
        <HelpCircle className={`w-3.5 h-3.5 ${userVote === 'PASS' ? 'text-white' : 'text-amber-600'}`} />
        <span>{passCount}</span>
      </button>
    </div>
  );
}
