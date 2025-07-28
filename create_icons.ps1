# PowerShell script to create basic launcher icons
Add-Type -AssemblyName System.Drawing

function Create-Icon {
    param(
        [string]$path,
        [int]$size,
        [string]$text,
        [bool]$isRound = $false
    )
    
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Fill background with a nice blue color
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(33, 150, 243))
    $graphics.FillRectangle($brush, 0, 0, $size, $size)
    
    # Add text
    $font = New-Object System.Drawing.Font("Arial", [math]::max(8, $size / 8), [System.Drawing.FontStyle]::Bold)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $textSize = $graphics.MeasureString($text, $font)
    $x = ($size - $textSize.Width) / 2
    $y = ($size - $textSize.Height) / 2
    $graphics.DrawString($text, $font, $textBrush, $x, $y)
    
    if ($isRound) {
        # Create circular mask for round icon
        $roundBitmap = New-Object System.Drawing.Bitmap($size, $size)
        $roundGraphics = [System.Drawing.Graphics]::FromImage($roundBitmap)
        $roundGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        
        # Create circular path
        $ellipse = New-Object System.Drawing.Drawing2D.GraphicsPath
        $ellipse.AddEllipse(0, 0, $size, $size)
        $roundGraphics.SetClip($ellipse)
        $roundGraphics.DrawImage($bitmap, 0, 0)
        
        $roundBitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        $roundGraphics.Dispose()
        $roundBitmap.Dispose()
        $ellipse.Dispose()
    } else {
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    
    $graphics.Dispose()
    $bitmap.Dispose()
    $brush.Dispose()
    $textBrush.Dispose()
    $font.Dispose()
}

# Create launcher icons for different densities
Create-Icon "app\src\main\res\mipmap-mdpi\ic_launcher.png" 48 "M"
Create-Icon "app\src\main\res\mipmap-hdpi\ic_launcher.png" 72 "M"
Create-Icon "app\src\main\res\mipmap-xhdpi\ic_launcher.png" 96 "M"
Create-Icon "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" 144 "M"
Create-Icon "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" 192 "M"

# Create round launcher icons
Create-Icon "app\src\main\res\mipmap-mdpi\ic_launcher_round.png" 48 "M" $true
Create-Icon "app\src\main\res\mipmap-hdpi\ic_launcher_round.png" 72 "M" $true
Create-Icon "app\src\main\res\mipmap-xhdpi\ic_launcher_round.png" 96 "M" $true
Create-Icon "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png" 144 "M" $true
Create-Icon "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png" 192 "M" $true

Write-Host "Icons created successfully!"
