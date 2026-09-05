@file:Suppress("ktlint:standard:max-line-length")

package com.goveye.app.ui.screens.party

/**
 * Static party history and colour explanation data.
 *
 * History text is condensed from Wikipedia. Colour explanations are adapted
 * from instantprint's "UK Political Party Colours & Logos" article
 * (https://www.instantprint.co.uk/printspiration/be-inspired/political-party-colours).
 *
 * This data is hardcoded rather than fetched at runtime because it is
 * editorial content that does not change frequently. It can be moved to
 * the seed DB later if needed.
 */
data class PartyHistory(val history: String? = null, val colourExplanation: String? = null)

val PARTY_HISTORY: Map<Int, PartyHistory> = mapOf(
    4 to PartyHistory(
        history = "The Conservative Party was founded in 1834 from the Tory Party and is one of the oldest political parties in the world. It has been the dominant force in British politics for much of the modern era, producing prime ministers from Disraeli and Churchill to Thatcher and Cameron. The party traditionally supports free markets, fiscal conservatism, and a strong national defence.",
        colourExplanation = "A popular hue for right-wing parties across the world, the Conservative Party has been associated with blue since its formation in 1834. Originally the Tories campaigned with the full spectrum of the Union Jack — red, blue and white — but dropped red after Labour adopted it. In marketing, blue instils a sense of trust, reliability and confidence."
    ),
    15 to PartyHistory(
        history = "The Labour Party was founded in 1900 as a political wing of the trade union movement to represent working-class interests. It grew to become one of the two dominant parties in British politics, replacing the Liberals. Key moments include the creation of the NHS under Attlee in 1948, the Wilson governments of the 1960s-70s, and the New Labour era under Blair from 1997. The party traditionally supports democratic socialism, workers' rights, and public services.",
        colourExplanation = "The colour red represents 'the blood of the angry workers' — those rising up against the bourgeoisie during the first French uprising in 1789. Red as a representation of the struggle between the working and upper classes may date back as far as the slave uprisings at the end of the Roman Empire, when revolutionaries brandished red flags as part of the rebellion."
    ),
    17 to PartyHistory(
        history = "The Liberal Democrats were formed in 1988 from the merger of the Liberal Party (itself tracing back to the Whigs of the 17th century) and the Social Democratic Party. The party has traditionally occupied the centre ground of British politics, supporting civil liberties, electoral reform, and European integration. The 2010-2015 coalition government with the Conservatives was a defining period.",
        colourExplanation = "The Liberal Democrats are a combination of two parties that merged in 1988 — the Liberal Party (whose colour was yellow) and the Social Democratic Party (rooted in socialism, hence red). Orange, as a mix of the two, symbolises the party's aim to balance social, economic, and political liberalism to create a more well-rounded party."
    ),
    44 to PartyHistory(
        history = "The Green Party of England and Wales was founded in 1990, splitting from the original Green Party (formed 1973 as PEOPLE, later the Ecology Party). The party's first MP, Caroline Lucas, was elected in 2010. It focuses on environmental sustainability, social justice, and progressive policies. The Green Party has its strongest support in university towns and urban areas.",
        colourExplanation = "The first recorded use of green in politics goes as far back as the 6th century — a faction in Constantinople during the Byzantine Empire. In the 1980s, green became the colour of many European parties whose agenda was focused on environmentalism, which is largely what we know it to signify now."
    ),
    22 to PartyHistory(
        history = "Plaid Cymru (Party of Wales) was founded in 1925 to advocate for Welsh self-government and the Welsh language. The party won its first parliamentary seat in 1966 and has been a consistent presence in Welsh politics, particularly in rural Welsh-speaking areas. It supports Welsh independence, environmentalism, and social democracy.",
        colourExplanation = "Plaid Cymru use a logo featuring a Welsh yellow poppy on a white and green background. Combining yellow and green represents a modern approach to politics, with the poppy showing the growth of the party and the Welsh people. Green represents growth and nature, while yellow signals hope for the future."
    ),
    29 to PartyHistory(
        history = "The Scottish National Party was founded in 1934 from the merger of the National Party of Scotland and the Scottish Party. It has campaigned for Scottish independence throughout its history, with the 2014 independence referendum being a defining moment. The SNP has been the dominant party in Scottish politics since the 2000s, governing in the Scottish Parliament and winning most Scottish seats at Westminster.",
        colourExplanation = "The SNP's use of yellow traces back to 1928 and the publication of David Lloyd George's report 'Britain's Industrial Future'. This report gained the nickname 'the Yellow Book' because many fictional books were printed with yellow covers during the 19th century to show they were something new. Yellow represents the new, the modern and the free."
    ),
    1036 to PartyHistory(
        history = "Reform UK was founded in 2018 as The Brexit Party by Nigel Farage, campaigning for the UK's withdrawal from the European Union. After Brexit was completed, the party was renamed Reform UK in January 2021 and pivoted to campaigning against net-zero policies, immigration, and what it calls 'woke' ideology. It won five seats in the 2024 general election, becoming a significant presence in British politics. The party was previously known as the Brexit Party (2019-2021) and briefly as Reform UK before its current incarnation.",
        colourExplanation = "Reform UK uses a distinctive turquoise blue, departing from the traditional party colour palette. The colour choice signals a break from establishment politics — neither the Conservative blue nor the Labour red — positioning the party as a fresh alternative. Turquoise conveys a sense of clarity and change."
    ),
    7 to PartyHistory(
        history = "The Democratic Unionist Party was founded in 1971 by Ian Paisley during the Troubles. It is the largest unionist party in Northern Ireland, supporting the union with the United Kingdom and socially conservative values. The party has been influential in Northern Irish politics and briefly held the balance of power at Westminster after the 2017 election.",
        colourExplanation = null
    ),
    30 to PartyHistory(
        history = "Sinn Féin was founded in 1905 and is the oldest Irish republican party. It campaigns for a united Ireland and the end of British rule in Northern Ireland. The party's MPs traditionally abstain from taking their seats in the House of Commons. In recent years, Sinn Féin has become the largest party in Northern Ireland and has expanded its presence in the Republic of Ireland.",
        colourExplanation = null
    ),
    31 to PartyHistory(
        history = "The Social Democratic and Labour Party was founded in 1970 during the early years of the Troubles. It is a centre-left, Irish nationalist party that supports a united Ireland through peaceful and constitutional means. The SDLP was the dominant nationalist party in Northern Ireland until Sinn Féin overtook it in the early 2000s.",
        colourExplanation = null
    ),
    1 to PartyHistory(
        history = "The Alliance Party of Northern Ireland was founded in 1970 as a non-sectarian, liberal alternative to the unionist and nationalist parties. It supports the cross-community power-sharing arrangements established by the Good Friday Agreement. The party has grown in recent years, particularly among voters who do not identify with the traditional unionist-nationalist divide.",
        colourExplanation = null
    ),
    38 to PartyHistory(
        history = "The Ulster Unionist Party was founded in 1905 and was the dominant party in Northern Ireland for most of the 20th century. It governed Northern Ireland from 1921 until the imposition of direct rule in 1972. The party has declined in recent decades, being overtaken by the DUP as the largest unionist party.",
        colourExplanation = null
    ),
    158 to PartyHistory(
        history = "Traditional Unionist Voice was founded in 2007 by Jim Allister after he left the DUP over power-sharing with Sinn Féin. The party opposes the Good Friday Agreement and power-sharing arrangements. It won its first seat in the UK Parliament in the 2024 general election.",
        colourExplanation = null
    )
)

fun getPartyHistory(partyId: Int): PartyHistory? = PARTY_HISTORY[partyId]
