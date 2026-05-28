import AppKit
import Foundation

let size = 1024
let output = CommandLine.arguments.count > 1
    ? CommandLine.arguments[1]
    : "app/src/main/res/drawable-nodpi/ic_launcher_image.png"

func color(_ hex: UInt32, _ alpha: CGFloat = 1) -> NSColor {
    NSColor(
        calibratedRed: CGFloat((hex >> 16) & 0xff) / 255,
        green: CGFloat((hex >> 8) & 0xff) / 255,
        blue: CGFloat(hex & 0xff) / 255,
        alpha: alpha
    )
}

func roundedRect(_ rect: CGRect, radius: CGFloat) -> NSBezierPath {
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
}

func drawText(_ text: String, center: CGPoint, size fontSize: CGFloat, color textColor: NSColor, glow: Bool = false) {
    let font = NSFont.boldSystemFont(ofSize: fontSize)
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = .center
    let attrs: [NSAttributedString.Key: Any] = [
        .font: font,
        .foregroundColor: textColor,
        .paragraphStyle: paragraph,
    ]
    let attributed = NSAttributedString(string: text, attributes: attrs)
    let bounds = attributed.boundingRect(
        with: CGSize(width: 180, height: 160),
        options: [.usesLineFragmentOrigin]
    )
    let rect = CGRect(
        x: center.x - 90,
        y: center.y - bounds.height / 2 - 6,
        width: 180,
        height: bounds.height + 16
    )

    if glow {
        NSGraphicsContext.current?.saveGraphicsState()
        NSShadow().apply {
            $0.shadowBlurRadius = 34
            $0.shadowColor = color(0x8B5CF6, 0.95)
            $0.shadowOffset = .zero
        }
        attributed.draw(in: rect)
        NSGraphicsContext.current?.restoreGraphicsState()
    }

    NSGraphicsContext.current?.saveGraphicsState()
    NSShadow().apply {
        $0.shadowBlurRadius = 8
        $0.shadowColor = color(0x000000, 0.75)
        $0.shadowOffset = CGSize(width: 0, height: -2)
    }
    attributed.draw(in: rect)
    NSGraphicsContext.current?.restoreGraphicsState()
}

extension NSShadow {
    func apply(_ block: (NSShadow) -> Void) {
        block(self)
        set()
    }
}

let image = NSImage(size: CGSize(width: size, height: size))
image.lockFocus()

NSGraphicsContext.current?.imageInterpolation = .high
color(0x070D13).setFill()
NSRect(x: 0, y: 0, width: size, height: size).fill()

let backgroundGradient = NSGradient(colors: [
    color(0x050910),
    color(0x0B151D),
    color(0x0B1017),
])!
backgroundGradient.draw(in: NSRect(x: 0, y: 0, width: size, height: size), angle: 45)

let card = CGRect(x: 106, y: 108, width: 812, height: 812)
NSGraphicsContext.current?.saveGraphicsState()
NSShadow().apply {
    $0.shadowBlurRadius = 42
    $0.shadowColor = color(0x000000, 0.52)
    $0.shadowOffset = CGSize(width: 0, height: -8)
}
let cardPath = roundedRect(card, radius: 168)
NSGradient(colors: [color(0x102936), color(0x071318), color(0x0B202A)])!
    .draw(in: cardPath, angle: -38)
NSGraphicsContext.current?.restoreGraphicsState()

color(0x16394A, 0.55).setStroke()
cardPath.lineWidth = 3
cardPath.stroke()

let purple = color(0x8B5CF6)
let violet = color(0xB17CFF)
let accentLine = NSBezierPath()
accentLine.move(to: CGPoint(x: 430, y: 862))
accentLine.line(to: CGPoint(x: 270, y: 862))
accentLine.curve(to: CGPoint(x: 170, y: 746), controlPoint1: CGPoint(x: 216, y: 862), controlPoint2: CGPoint(x: 170, y: 812))
accentLine.line(to: CGPoint(x: 170, y: 617))
violet.setStroke()
accentLine.lineWidth = 15
accentLine.lineCapStyle = .round
accentLine.stroke()

let lowerLine = NSBezierPath()
lowerLine.move(to: CGPoint(x: 596, y: 198))
lowerLine.line(to: CGPoint(x: 762, y: 198))
lowerLine.curve(to: CGPoint(x: 856, y: 304), controlPoint1: CGPoint(x: 824, y: 198), controlPoint2: CGPoint(x: 856, y: 240))
lowerLine.line(to: CGPoint(x: 856, y: 438))
violet.setStroke()
lowerLine.lineWidth = 15
lowerLine.lineCapStyle = .round
lowerLine.stroke()

for row in 0..<3 {
    for col in 0..<2 {
        color(0x7C5CFF, 0.72).setFill()
        NSBezierPath(ovalIn: CGRect(x: 808 + col * 32, y: 744 - row * 32, width: 14, height: 14)).fill()
        color(0x3C7A87, 0.64).setFill()
        NSBezierPath(ovalIn: CGRect(x: 174 + col * 32, y: 246 - row * 32, width: 14, height: 14)).fill()
    }
}

let grid = CGRect(x: 236, y: 254, width: 552, height: 560)
let gridPath = roundedRect(grid, radius: 58)
color(0x08131B, 0.20).setFill()
gridPath.fill()
color(0x465E6E, 0.82).setStroke()
gridPath.lineWidth = 4
gridPath.stroke()

let cellW = grid.width / 3
let cellH = grid.height / 3
for i in 1...2 {
    let x = grid.minX + CGFloat(i) * cellW
    let vertical = NSBezierPath()
    vertical.move(to: CGPoint(x: x, y: grid.minY))
    vertical.line(to: CGPoint(x: x, y: grid.maxY))
    vertical.lineWidth = 6
    color(0x526B79, 0.86).setStroke()
    vertical.stroke()

    let y = grid.minY + CGFloat(i) * cellH
    let horizontal = NSBezierPath()
    horizontal.move(to: CGPoint(x: grid.minX, y: y))
    horizontal.line(to: CGPoint(x: grid.maxX, y: y))
    horizontal.lineWidth = 6
    color(0x526B79, 0.86).setStroke()
    horizontal.stroke()
}

let centerCell = CGRect(x: grid.minX + cellW, y: grid.minY + cellH, width: cellW, height: cellH)
NSGraphicsContext.current?.saveGraphicsState()
NSShadow().apply {
    $0.shadowBlurRadius = 42
    $0.shadowColor = color(0x8B5CF6, 0.95)
    $0.shadowOffset = .zero
}
let centerPath = roundedRect(centerCell.insetBy(dx: 9, dy: 9), radius: 24)
color(0x33226D, 0.88).setFill()
centerPath.fill()
purple.setStroke()
centerPath.lineWidth = 4
centerPath.stroke()
NSGraphicsContext.current?.restoreGraphicsState()

let digits = [
    ["1", "6", "5"],
    ["7", "9", "3"],
    ["4", "2", "8"],
]
for row in 0..<3 {
    for col in 0..<3 {
        let cx = grid.minX + CGFloat(col) * cellW + cellW / 2
        let cy = grid.maxY - CGFloat(row) * cellH - cellH / 2 - 10
        drawText(
            digits[row][col],
            center: CGPoint(x: cx, y: cy),
            size: row == 1 && col == 1 ? 116 : 112,
            color: row == 1 && col == 1 ? color(0xF7F2FF) : color(0xCFD5DC),
            glow: row == 1 && col == 1
        )
    }
}

image.unlockFocus()

guard let tiff = image.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff),
      let png = bitmap.representation(using: .png, properties: [:]) else {
    fatalError("Failed to render PNG")
}

let outputDirectory = (output as NSString).deletingLastPathComponent
if !outputDirectory.isEmpty {
    try FileManager.default.createDirectory(
        atPath: outputDirectory,
        withIntermediateDirectories: true
    )
}
try png.write(to: URL(fileURLWithPath: output))
