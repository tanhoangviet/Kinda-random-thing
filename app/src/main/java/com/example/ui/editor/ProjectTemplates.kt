package com.example.ui.editor

import com.example.data.model.*

object ProjectTemplates {

    fun createEmptyProject(): RobloxObject {
        val screenGui = createDefaultObject(RobloxClass.ScreenGui, "MainGui")
        
        val mainFrame = createDefaultObject(RobloxClass.Frame, "MainFrame")
        val mainFrameProps = mainFrame.properties.toMutableMap()
        mainFrameProps["Size"] = UDim2(0.4f, 0, 0.4f, 0)
        mainFrameProps["Position"] = UDim2(0.3f, 0, 0.3f, 0)
        mainFrameProps["BackgroundColor3"] = Color3(30, 30, 30)
        mainFrameProps["BorderSizePixel"] = 0
        
        val uiCorner = createDefaultObject(RobloxClass.UICorner, "FrameCorner")
        val uiCornerProps = uiCorner.properties.toMutableMap()
        uiCornerProps["CornerRadius"] = UDim2(0f, 12, 0f, 12)
        
        val titleLabel = createDefaultObject(RobloxClass.TextLabel, "TitleText")
        val titleLabelProps = titleLabel.properties.toMutableMap()
        titleLabelProps["Size"] = UDim2(1.0f, 0, 0.25f, 0)
        titleLabelProps["Position"] = UDim2(0f, 0, 0f, 0)
        titleLabelProps["Text"] = "My Custom GUI"
        titleLabelProps["TextSize"] = 18
        titleLabelProps["Font"] = "SourceSansBold"
        titleLabelProps["TextColor3"] = Color3(255, 255, 255)
        
        return screenGui.copy(
            children = listOf(
                mainFrame.copy(
                    properties = mainFrameProps,
                    children = listOf(
                        uiCorner.copy(properties = uiCornerProps),
                        titleLabel.copy(properties = titleLabelProps)
                    )
                )
            )
        )
    }

    fun createMainMenu(): RobloxObject {
        val screenGui = createDefaultObject(RobloxClass.ScreenGui, "MainMenuGui")
        
        // Background overlay
        val background = createDefaultObject(RobloxClass.Frame, "OverlayFrame")
        val bgProps = background.properties.toMutableMap()
        bgProps["Size"] = UDim2(1.0f, 0, 1.0f, 0)
        bgProps["Position"] = UDim2(0f, 0, 0f, 0)
        bgProps["BackgroundColor3"] = Color3(15, 15, 20)
        bgProps["BackgroundTransparency"] = 0.15f
        bgProps["BorderSizePixel"] = 0
        
        // Centered Main Panel
        val panel = createDefaultObject(RobloxClass.Frame, "MenuPanel")
        val panelProps = panel.properties.toMutableMap()
        panelProps["Size"] = UDim2(0.35f, 0, 0.7f, 0)
        panelProps["Position"] = UDim2(0.325f, 0, 0.15f, 0)
        panelProps["BackgroundColor3"] = Color3(25, 25, 30)
        panelProps["BorderSizePixel"] = 0
        
        val panelCorner = createDefaultObject(RobloxClass.UICorner, "PanelCorner")
        val pcProps = panelCorner.properties.toMutableMap()
        pcProps["CornerRadius"] = UDim2(0f, 16, 0f, 16)
        
        val panelStroke = createDefaultObject(RobloxClass.UIStroke, "PanelStroke")
        val psProps = panelStroke.properties.toMutableMap()
        psProps["Color"] = Color3(0, 162, 255)
        psProps["Thickness"] = 2
        
        // Logo Title
        val title = createDefaultObject(RobloxClass.TextLabel, "GameLogo")
        val titleProps = title.properties.toMutableMap()
        titleProps["Size"] = UDim2(0.9f, 0, 0.2f, 0)
        titleProps["Position"] = UDim2(0.05f, 0, 0.05f, 0)
        titleProps["Text"] = "BLOXY LEGENDS"
        titleProps["TextColor3"] = Color3(255, 215, 0) // Gold
        titleProps["TextSize"] = 24
        titleProps["Font"] = "SourceSansBold"
        titleProps["TextScaled"] = true
        titleProps["RichText"] = true
        
        // Subtitle
        val subtitle = createDefaultObject(RobloxClass.TextLabel, "Subtitle")
        val subProps = subtitle.properties.toMutableMap()
        subProps["Size"] = UDim2(0.9f, 0, 0.08f, 0)
        subProps["Position"] = UDim2(0.05f, 0, 0.22f, 0)
        subProps["Text"] = "v1.0.4 Beta Edition"
        subProps["TextColor3"] = Color3(160, 160, 160)
        subProps["TextSize"] = 12
        subProps["Font"] = "SourceSansItalic"
        
        // Buttons container
        val btnContainer = createDefaultObject(RobloxClass.Frame, "ButtonsGroup")
        val bcProps = btnContainer.properties.toMutableMap()
        bcProps["Size"] = UDim2(0.85f, 0, 0.55f, 0)
        bcProps["Position"] = UDim2(0.075f, 0, 0.35f, 0)
        bcProps["BackgroundTransparency"] = 1.0f
        bcProps["BorderSizePixel"] = 0
        
        val listLayout = createDefaultObject(RobloxClass.UIListLayout, "VerticalList")
        val llProps = listLayout.properties.toMutableMap()
        llProps["FillDirection"] = "Vertical"
        llProps["HorizontalAlignment"] = "Center"
        llProps["VerticalAlignment"] = "Top"
        llProps["Padding"] = UDim2(0f, 0, 0f, 10)
        
        // Play Button
        val playBtn = createDefaultObject(RobloxClass.TextButton, "PlayButton")
        val playProps = playBtn.properties.toMutableMap()
        playProps["Size"] = UDim2(1.0f, 0, 0.24f, 0)
        playProps["Text"] = "PLAY"
        playProps["BackgroundColor3"] = Color3(46, 204, 113) // Green
        playProps["Font"] = "SourceSansBold"
        playProps["TextSize"] = 16
        val playCorner = createDefaultObject(RobloxClass.UICorner, "PlayCorner").let {
            val p = it.properties.toMutableMap()
            p["CornerRadius"] = UDim2(0f, 8, 0f, 8)
            it.copy(properties = p)
        }
        
        // Shop Button
        val shopBtn = createDefaultObject(RobloxClass.TextButton, "ShopButton")
        val shopProps = shopBtn.properties.toMutableMap()
        shopProps["Size"] = UDim2(1.0f, 0, 0.24f, 0)
        shopProps["Text"] = "SHOP"
        shopProps["BackgroundColor3"] = Color3(52, 152, 219) // Blue
        shopProps["Font"] = "SourceSansBold"
        shopProps["TextSize"] = 16
        val shopCorner = createDefaultObject(RobloxClass.UICorner, "ShopCorner").let {
            val p = it.properties.toMutableMap()
            p["CornerRadius"] = UDim2(0f, 8, 0f, 8)
            it.copy(properties = p)
        }
        
        // Settings Button
        val settingsBtn = createDefaultObject(RobloxClass.TextButton, "SettingsButton")
        val setProps = settingsBtn.properties.toMutableMap()
        setProps["Size"] = UDim2(1.0f, 0, 0.24f, 0)
        setProps["Text"] = "SETTINGS"
        setProps["BackgroundColor3"] = Color3(127, 140, 141) // Gray
        setProps["Font"] = "SourceSansBold"
        setProps["TextSize"] = 16
        val setCorner = createDefaultObject(RobloxClass.UICorner, "SettingsCorner").let {
            val p = it.properties.toMutableMap()
            p["CornerRadius"] = UDim2(0f, 8, 0f, 8)
            it.copy(properties = p)
        }
        
        return screenGui.copy(
            children = listOf(
                background.copy(
                    properties = bgProps,
                    children = listOf(
                        panel.copy(
                            properties = panelProps,
                            children = listOf(
                                panelCorner.copy(properties = pcProps),
                                panelStroke.copy(properties = psProps),
                                title.copy(properties = titleProps),
                                subtitle.copy(properties = subProps),
                                btnContainer.copy(
                                    properties = bcProps,
                                    children = listOf(
                                        listLayout.copy(properties = llProps),
                                        playBtn.copy(properties = playProps, children = listOf(playCorner)),
                                        shopBtn.copy(properties = shopProps, children = listOf(shopCorner)),
                                        settingsBtn.copy(properties = setProps, children = listOf(setCorner))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    fun createShopUI(): RobloxObject {
        val screenGui = createDefaultObject(RobloxClass.ScreenGui, "ShopGui")
        
        val shopPanel = createDefaultObject(RobloxClass.Frame, "ShopPanel")
        val panelProps = shopPanel.properties.toMutableMap()
        panelProps["Size"] = UDim2(0.55f, 0, 0.75f, 0)
        panelProps["Position"] = UDim2(0.225f, 0, 0.125f, 0)
        panelProps["BackgroundColor3"] = Color3(20, 20, 25)
        panelProps["BorderSizePixel"] = 0
        
        val panelCorner = createDefaultObject(RobloxClass.UICorner, "ShopCorner").let {
            val p = it.properties.toMutableMap()
            p["CornerRadius"] = UDim2(0f, 14, 0f, 14)
            it.copy(properties = p)
        }
        
        val stroke = createDefaultObject(RobloxClass.UIStroke, "ShopBorder").let {
            val p = it.properties.toMutableMap()
            p["Color"] = Color3(255, 170, 0) // Amber Accent
            p["Thickness"] = 2
            it.copy(properties = p)
        }
        
        // Header Label
        val header = createDefaultObject(RobloxClass.TextLabel, "HeaderLabel")
        val headerProps = header.properties.toMutableMap()
        headerProps["Size"] = UDim2(0.9f, 0, 0.12f, 0)
        headerProps["Position"] = UDim2(0.05f, 0, 0.04f, 0)
        headerProps["Text"] = "IN-GAME MERCHANDISE"
        headerProps["TextColor3"] = Color3(255, 255, 255)
        headerProps["Font"] = "SourceSansBold"
        headerProps["TextSize"] = 18
        headerProps["TextXAlignment"] = "Left"
        
        // Close Button
        val closeBtn = createDefaultObject(RobloxClass.TextButton, "CloseButton")
        val closeProps = closeBtn.properties.toMutableMap()
        closeProps["Size"] = UDim2(0.08f, 0, 0.1f, 0)
        closeProps["Position"] = UDim2(0.87f, 0, 0.04f, 0)
        closeProps["Text"] = "X"
        closeProps["TextColor3"] = Color3(255, 100, 100)
        closeProps["Font"] = "SourceSansBold"
        closeProps["TextSize"] = 18
        closeProps["BackgroundColor3"] = Color3(50, 20, 20)
        val closeCorner = createDefaultObject(RobloxClass.UICorner, "CloseCorner").let {
            val p = it.properties.toMutableMap()
            p["CornerRadius"] = UDim2(0f, 6, 0f, 6)
            it.copy(properties = p)
        }
        
        // Scrolling Container for item grid
        val scroll = createDefaultObject(RobloxClass.ScrollingFrame, "ItemGridScroll")
        val sProps = scroll.properties.toMutableMap()
        sProps["Size"] = UDim2(0.9f, 0, 0.75f, 0)
        sProps["Position"] = UDim2(0.05f, 0, 0.18f, 0)
        sProps["BackgroundTransparency"] = 1.0f
        sProps["BorderSizePixel"] = 0
        sProps["CanvasSize"] = UDim2(0f, 0, 1.8f, 0)
        sProps["ScrollBarThickness"] = 6
        
        val gridLayout = createDefaultObject(RobloxClass.UIGridLayout, "ShopGrid")
        val glProps = gridLayout.properties.toMutableMap()
        glProps["CellSize"] = UDim2(0.28f, 0, 0.4f, 0) // Relative size grid cards
        glProps["CellPadding"] = UDim2(0.04f, 0, 0.05f, 0)
        
        // Mock items
        val itemCards = (1..6).map { i ->
            val itemCard = createDefaultObject(RobloxClass.Frame, "ItemCard_$i")
            val cardProps = itemCard.properties.toMutableMap()
            cardProps["BackgroundColor3"] = Color3(35, 35, 42)
            cardProps["BorderSizePixel"] = 0
            
            val cardCorner = createDefaultObject(RobloxClass.UICorner, "CardCorner").let {
                val p = it.properties.toMutableMap()
                p["CornerRadius"] = UDim2(0f, 10, 0f, 10)
                it.copy(properties = p)
            }
            
            val itemTitle = createDefaultObject(RobloxClass.TextLabel, "ItemTitle")
            val tProps = itemTitle.properties.toMutableMap()
            tProps["Size"] = UDim2(0.9f, 0, 0.25f, 0)
            tProps["Position"] = UDim2(0.05f, 0, 0.05f, 0)
            tProps["Text"] = when(i) {
                1 -> "Laser Rifle"
                2 -> "Speed Boots"
                3 -> "Shield Potion"
                4 -> "Gravity Coil"
                5 -> "Medkit"
                else -> "Jetpack"
            }
            tProps["TextColor3"] = Color3(240, 240, 240)
            tProps["Font"] = "SourceSansBold"
            tProps["TextSize"] = 12
            
            val buyBtn = createDefaultObject(RobloxClass.TextButton, "BuyButton")
            val bProps = buyBtn.properties.toMutableMap()
            bProps["Size"] = UDim2(0.8f, 0, 0.25f, 0)
            bProps["Position"] = UDim2(0.1f, 0, 0.7f, 0)
            bProps["Text"] = "Buy - " + (i * 150) + "R$"
            bProps["BackgroundColor3"] = Color3(255, 170, 0)
            bProps["TextColor3"] = Color3(20, 20, 20)
            bProps["Font"] = "SourceSansBold"
            bProps["TextSize"] = 11
            val buyCorner = createDefaultObject(RobloxClass.UICorner, "BuyCorner").let {
                val p = it.properties.toMutableMap()
                p["CornerRadius"] = UDim2(0f, 6, 0f, 6)
                it.copy(properties = p)
            }
            
            itemCard.copy(
                properties = cardProps,
                children = listOf(
                    cardCorner,
                    itemTitle.copy(properties = tProps),
                    buyBtn.copy(properties = bProps, children = listOf(buyCorner))
                )
            )
        }
        
        return screenGui.copy(
            children = listOf(
                shopPanel.copy(
                    properties = panelProps,
                    children = listOf(
                        panelCorner,
                        stroke,
                        header.copy(properties = headerProps),
                        closeBtn.copy(properties = closeProps, children = listOf(closeCorner)),
                        scroll.copy(
                            properties = sProps,
                            children = listOf(gridLayout.copy(properties = glProps)) + itemCards
                        )
                    )
                )
            )
        )
    }

    fun createStatusHUD(): RobloxObject {
        val screenGui = createDefaultObject(RobloxClass.ScreenGui, "StatusHUD")
        
        val hudContainer = createDefaultObject(RobloxClass.Frame, "HUDContainer")
        val contProps = hudContainer.properties.toMutableMap()
        contProps["Size"] = UDim2(0.24f, 0, 0.12f, 0)
        contProps["Position"] = UDim2(0.02f, 0, 0.02f, 0)
        contProps["BackgroundColor3"] = Color3(15, 15, 15)
        contProps["BackgroundTransparency"] = 0.3f
        contProps["BorderSizePixel"] = 0
        
        val corner = createDefaultObject(RobloxClass.UICorner, "HUDCorner").let {
            val p = it.properties.toMutableMap()
            p["CornerRadius"] = UDim2(0f, 12, 0f, 12)
            it.copy(properties = p)
        }
        
        val label = createDefaultObject(RobloxClass.TextLabel, "PlayerName")
        val labelProps = label.properties.toMutableMap()
        labelProps["Size"] = UDim2(0.6f, 0, 0.4f, 0)
        labelProps["Position"] = UDim2(0.08f, 0, 0.1f, 0)
        labelProps["Text"] = "Guest_3499"
        labelProps["TextColor3"] = Color3(255, 255, 255)
        labelProps["Font"] = "SourceSansBold"
        labelProps["TextSize"] = 12
        labelProps["TextXAlignment"] = "Left"
        
        val hpBarBg = createDefaultObject(RobloxClass.Frame, "HPBarBackground")
        val hbgProps = hpBarBg.properties.toMutableMap()
        hbgProps["Size"] = UDim2(0.84f, 0, 0.25f, 0)
        hbgProps["Position"] = UDim2(0.08f, 0, 0.55f, 0)
        hbgProps["BackgroundColor3"] = Color3(60, 20, 20)
        hbgProps["BorderSizePixel"] = 0
        
        val barCorner = createDefaultObject(RobloxClass.UICorner, "BarCorner").let {
            val p = it.properties.toMutableMap()
            p["CornerRadius"] = UDim2(0f, 4, 0f, 4)
            it.copy(properties = p)
        }
        
        val hpBarFill = createDefaultObject(RobloxClass.Frame, "HPBarFill")
        val hfillProps = hpBarFill.properties.toMutableMap()
        hfillProps["Size"] = UDim2(0.75f, 0, 1.0f, 0) // 75% Health
        hfillProps["BackgroundColor3"] = Color3(231, 76, 60) // Red-orange HP
        hfillProps["BorderSizePixel"] = 0
        
        val hpText = createDefaultObject(RobloxClass.TextLabel, "HPLabel")
        val hpTextProps = hpText.properties.toMutableMap()
        hpTextProps["Size"] = UDim2(1.0f, 0, 1.0f, 0)
        hpTextProps["Position"] = UDim2(0f, 0, 0f, 0)
        hpTextProps["Text"] = "75/100"
        hpTextProps["TextColor3"] = Color3(255, 255, 255)
        hpTextProps["Font"] = "SourceSansBold"
        hpTextProps["TextSize"] = 9
        hpTextProps["BackgroundTransparency"] = 1.0f
        
        return screenGui.copy(
            children = listOf(
                hudContainer.copy(
                    properties = contProps,
                    children = listOf(
                        corner,
                        label.copy(properties = labelProps),
                        hpBarBg.copy(
                            properties = hbgProps,
                            children = listOf(
                                barCorner,
                                hpBarFill.copy(
                                    properties = hfillProps,
                                    children = listOf(barCorner)
                                ),
                                hpText.copy(properties = hpTextProps)
                            )
                        )
                    )
                )
            )
        )
    }

    fun getTemplateNames(): List<String> {
        return listOf("Empty Screen", "Main Menu Dashboard", "Item Shop Grid", "Player HUD Status")
    }

    fun createTemplateByName(name: String): RobloxObject {
        return when (name) {
            "Main Menu Dashboard" -> createMainMenu()
            "Item Shop Grid" -> createShopUI()
            "Player HUD Status" -> createStatusHUD()
            else -> createEmptyProject()
        }
    }
}
