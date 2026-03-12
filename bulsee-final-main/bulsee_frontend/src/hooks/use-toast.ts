export function useToast() {
  const toast = ({ title, description, variant }: any) => {
    console.log(`[Toast] ${title}: ${description}`);
    if (variant === "destructive") alert(`🚨 ${title}\n${description}`);
  }
  return { toast }
}
