import * as React from "react"
import { cn } from "@/lib/utils"

const Button = React.forwardRef<HTMLButtonElement, React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "default" | "ghost" | "outline", size?: "default" | "sm" | "icon" }>(
  ({ className, variant = "default", size = "default", ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          "inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none disabled:pointer-events-none disabled:opacity-50",
          variant === "default" && "bg-blue-600 text-white hover:bg-blue-700",
          variant === "ghost" && "hover:bg-gray-100 text-gray-700",
          variant === "outline" && "border border-gray-200 bg-white hover:bg-gray-100",
          size === "default" && "h-10 px-4 py-2",
          size === "sm" && "h-9 px-3",
          size === "icon" && "h-10 w-10",
          className
        )}
        {...props}
      />
    )
  }
)
Button.displayName = "Button"
export { Button }