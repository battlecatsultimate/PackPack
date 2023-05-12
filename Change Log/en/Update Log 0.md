# Change Log #0

---

<div align="right">

| 🌐 Language |
| :---: |  
| [🇰🇷 한국어](https://github.com/battlecatsultimate/PackPack/edit/main/Change%20Log/kr/Update%20Log%200.md) |

</div>

## Treasure System

### Introducing the system

Until now, the bot considered that users always have full treasure collected. But this isn't always ture, and some of 
them may have non-full treasure due to their account's progression. To solve such problem, I added customizable treasure
system into the bot. Users can put their own personal treasure status into the bot, and make the bot use assigned 
treasure when displaying stats.

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106398501220405268/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                       Unit stats embed showing damage with considering treasure                       |                                             

</div>

For example, with proper condition, bot will display damage or HP with considering treasure and abilities. 

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106399160279781427/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                       Alien enemy embed showing data with considering treasure                        |

</div>

Another example is that it can consider ItF/CotC crystal treasure status, so even though you don't manually set the 
magnification, bot will handle that automatically. As you can see the picture above, user set the magnification as 100%,
but bot automatically set magnification to 700% because user assigned custom treasure with 0% ItF crystal treasure.

---

### Configuring Treasure

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106403736382869564/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                                       Treasure setting command                                        |

</div>

You can customize your treasure by calling `p!treasure` or `p!tr`. If you call the command, you will see your current 
treasure setting. The bot doesn't include unnecessary treasures such as cannon-related treasures because they won't be 
used in any commands. Treasures are categorized by; 

- Basic Upgrades
- EoC Treasures
- ItF Treasures
- CotC Treasures

You can select the button below to adjust each section's levels/treasures.

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106403792896938054/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                                       Treasure adjustment panel                                       |

</div>

Like the picture above, you can put your current account's treasure setting, and submit the panel, then bot will handle 
the data. The reason of being 300% in the picture is that there are 3 chapters in total. You can calculate the 
percentage by summing up `Power` like the picture below.

<div align="center">

| <image width=50% height=50% src="https://static.wikia.nocookie.net/battle-cats/images/5/58/Energy_Drink_Activated.png"> |
|:--------------------------------------------------------------------------------------------------:|
|                                     Treasure power in the game                                     |

</div>

---

### Activating treasure consideration

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106406066251649054/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                                    Stat with considering treasure                                     |

</div>

In `p!fs` command, if you pass `-tr` argument, bot will display some stat values with considering the treasure together.
There can be conflict that unit targets multiple traits, while user has different fruit treasures. In this case, the bot 
will find the treasure that can give maximum effect, and applies only that one. Since multiplier for abilities such as 
`Massive Damage`, or `Resistant` can be defined, bot will display HP and damage with considering said abilities as well.

Other values such as cooldown or drop from enemies are considered automatically. Bot notices users whether this stat 
embed applies custom treasure or not. If full treasure is applied, it won't display such text in the embed. 

---

### Configuring the treasure

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106398373227004024/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                                    Treasure in user configuration                                     |

</div>

You can also make bot always use `-tr` argument when calling `p!fs` command via `p!config` command.

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106409405387391087/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                                  Forcing users to use full treasure                                   |

</div>

One of the biggest problem that can happen due to implementation of treasure system is that bot may show 
different stats for each users due to their own customized treasure setting, making users confused each other. 
In this case, moderators can force users to always use full treasure via `p!serverconfig` command as well. **Users still
can use customized treasure in DM of the bot, so keep in mind this.**

---

## Improvement of `p!config`

<div align="true">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106410285834719313/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                                   Switch component in configuration                                   |

</div>

Bot shows list for switchable configuration, having `True` and `False` as list elements. This has been changed to the 
button which acts like switch now.

---

## Displaying LD/Omni Icon

<div align="center">

| <image src="https://cdn.discordapp.com/attachments/642008685199228946/1106410947351945226/image.png"> |
|:-----------------------------------------------------------------------------------------------------:|
|                                       Bot showing LD/Omni icon                                        |

</div>

