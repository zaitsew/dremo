// Draws the Android TV launcher banner (320x180 px, xhdpi), the Play Store TV
// banner (1280x720 px), the Play feature graphic (1024x500 px) and the Play
// icon (512x512 px): a crescent moon over two soft waves on a night-indigo sky.
// Run on a Mac from the repo root: swift tools/make-banner.swift
import AppKit

let moonColor = NSColor(red: 0.99, green: 0.90, blue: 0.54, alpha: 1)   // #FDE68A
let waveColor = NSColor(red: 0.65, green: 0.71, blue: 0.99, alpha: 1)   // #A5B4FC
let skyTop = NSColor(red: 0.02, green: 0.03, blue: 0.08, alpha: 1)
let skyBottom = NSColor(red: 0.12, green: 0.11, blue: 0.29, alpha: 1)   // #1E1B4B

/// Draws the moon-and-waves mark centred at (cx, cy) in a box of `size` points.
func drawMark(_ ctx: CGContext, cx: CGFloat, cy: CGFloat, size: CGFloat) {
    let u = size / 100
    // Glow behind the moon.
    let glow = NSGradient(colors: [moonColor.withAlphaComponent(0.35), moonColor.withAlphaComponent(0)])!
    glow.draw(fromCenter: NSPoint(x: cx - 4 * u, y: cy + 8 * u), radius: 0,
              toCenter: NSPoint(x: cx - 4 * u, y: cy + 8 * u), radius: 48 * u, options: [])
    // Crescent: a disc minus a shifted disc, by even-odd fill.
    let outer = CGRect(x: cx - 26 * u, y: cy - 18 * u, width: 52 * u, height: 52 * u)
    let bite = CGRect(x: cx - 10 * u, y: cy - 8 * u, width: 46 * u, height: 46 * u)
    ctx.saveGState()
    ctx.addEllipse(in: outer)
    ctx.clip()
    ctx.addRect(outer)
    ctx.addEllipse(in: bite)
    ctx.setFillColor(moonColor.cgColor)
    ctx.fillPath(using: .evenOdd)
    ctx.restoreGState()
    // Two waves.
    ctx.setLineCap(.round)
    ctx.setStrokeColor(waveColor.cgColor)
    for (i, y) in [cy - 30 * u, cy - 42 * u].enumerated() {
        ctx.setLineWidth(5 * u)
        ctx.setAlpha(i == 0 ? 1 : 0.55)
        let x0 = cx - 34 * u, x1 = cx + 34 * u
        ctx.move(to: CGPoint(x: x0, y: y))
        ctx.addCurve(to: CGPoint(x: cx, y: y),
                     control1: CGPoint(x: x0 + 12 * u, y: y + 9 * u),
                     control2: CGPoint(x: cx - 12 * u, y: y - 9 * u))
        ctx.addCurve(to: CGPoint(x: x1, y: y),
                     control1: CGPoint(x: cx + 12 * u, y: y + 9 * u),
                     control2: CGPoint(x: x1 - 12 * u, y: y - 9 * u))
        ctx.strokePath()
    }
    ctx.setAlpha(1)
}

func stars(_ ctx: CGContext, width: CGFloat, height: CGFloat, count: Int, avoid: CGRect) {
    var seed: UInt64 = 42
    func next() -> CGFloat {
        seed = seed &* 6364136223846793005 &+ 1442695040888963407
        return CGFloat((seed >> 33) % 10_000) / 10_000
    }
    for _ in 0..<count {
        let x = next() * width, y = next() * height, r = (0.4 + next() * 1.2) * height / 180
        if avoid.contains(CGPoint(x: x, y: y)) { continue }
        ctx.setFillColor(NSColor.white.withAlphaComponent(0.25 + next() * 0.6).cgColor)
        ctx.fillEllipse(in: CGRect(x: x - r, y: y - r, width: 2 * r, height: 2 * r))
    }
}

func render(width: Int, height: Int, iconOnly: Bool = false, to path: String) {
    let w = CGFloat(width), h = CGFloat(height)
    let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil, pixelsWide: width, pixelsHigh: height, bitsPerSample: 8,
        samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
        bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    let ctx = NSGraphicsContext.current!.cgContext

    NSGradient(starting: skyBottom, ending: skyTop)!
        .draw(in: NSRect(x: 0, y: 0, width: w, height: h), angle: 90)

    if iconOnly {
        stars(ctx, width: w, height: h, count: 40, avoid: CGRect(x: w * 0.2, y: h * 0.1, width: w * 0.6, height: h * 0.8))
        drawMark(ctx, cx: w / 2, cy: h * 0.52, size: w * 0.8)
    } else {
        // Layout is drawn on a 320x180 grid, scaled to the height and centred.
        let s = h / 180
        let markCx = w / 2 - 74 * s
        stars(ctx, width: w, height: h, count: 90, avoid: CGRect(x: markCx - 60 * s, y: 20 * s, width: 250 * s, height: 140 * s))
        drawMark(ctx, cx: markCx, cy: h * 0.5, size: 120 * s)
        let title: [NSAttributedString.Key: Any] = [
            .font: NSFont.systemFont(ofSize: 46 * s, weight: .bold), .foregroundColor: NSColor.white]
        NSAttributedString(string: "Dremo", attributes: title)
            .draw(at: NSPoint(x: w / 2 - 30 * s, y: h * 0.5 - 4 * s))
        let sub: [NSAttributedString.Key: Any] = [
            .font: NSFont.systemFont(ofSize: 14.5 * s, weight: .medium), .foregroundColor: waveColor]
        NSAttributedString(string: "Calm TV screen", attributes: sub)
            .draw(at: NSPoint(x: w / 2 - 28 * s, y: h * 0.5 - 24 * s))
    }
    NSGraphicsContext.restoreGraphicsState()
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: path))
}

render(width: 320, height: 180, to: "app/src/main/res/drawable-xhdpi/banner.png")
render(width: 1280, height: 720, to: "docs/play/tv-banner-1280x720.png")
render(width: 1024, height: 500, to: "docs/play/feature-graphic-1024x500.png")
render(width: 512, height: 512, iconOnly: true, to: "docs/play/icon-512.png")
