package acr.browser.lightning.html.homepage

import com.anthonycr.mezzanine.FileStream

/**
 * The store for the homepage HTML.
 */
@FileStream("app/src/main/html/index_en.html")
interface HomePageReader {

    fun provideHtml(): String

}