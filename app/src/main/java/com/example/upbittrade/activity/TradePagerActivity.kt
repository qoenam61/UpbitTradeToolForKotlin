package com.example.upbittrade.activity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.upbittrade.R
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.TradeViewModel
import com.example.upbittrade.utils.PreferenceUtil

@Suppress("PrivatePropertyName")
class TradePagerActivity : FragmentActivity() {

    companion object {
        const val TAG = "DefaultViewModel"
        var ACCESS_KEY : String? = null
        var SECRET_KEY : String? = null
    }

    enum class PostType {
        MARKETS_INFO,
        POST_ORDER_INFO,
        DELETE_ORDER_INFO,
        MIN_CANDLE_INFO,
        DAY_CANDLE_INFO,
        WEEK_CANDLE_INFO,
        MONTH_CANDLE_INFO,
        ACCOUNTS_INFO,
        CHANCE_INFO,
        TICKER_INFO,
        TRADE_INFO,
        SEARCH_ORDER_INFO,
        CHECK_ORDER_INFO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_pager)

        val preferenceUtil = PreferenceUtil(this)
        ACCESS_KEY = preferenceUtil.getString(PreferenceUtil.ACCESS_KEY, "")
        SECRET_KEY = preferenceUtil.getString(PreferenceUtil.SECRET_KEY, "")

        val viewPager = findViewById<ViewPager2>(R.id.pager)
        viewPager.adapter = ScreenSlidePagerAdapter(this, TradeFragment())
        viewPager.setPageTransformer(ZoomOutPageTransformer())
    }

    inner class ScreenSlidePagerAdapter(activity: TradePagerActivity, private val tradeFragment: TradeFragment): FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return 1
        }

        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> tradeFragment
                else -> tradeFragment
            }

        }

    }

   inner class ZoomOutPageTransformer: ViewPager2.PageTransformer {
       private val MIN_SCALE = 0.85f
       private val MIN_ALPHA = 0.5f

       override fun transformPage(page: View, position: Float) {
           val pageWidth: Int = page.width
           val pageHeight: Int = page.height

           when {
               position < -1 -> { // [-Infinity,-1)
                   // This page is way off-screen to the left.
                   page.alpha = 0f
               }
               position <= 1 -> { // [-1,1]
                   // Modify the default slide transition to shrink the page as well
                   val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                   val vertMargin = pageHeight * (1 - scaleFactor) / 2
                   val horzMargin = pageWidth * (1 - scaleFactor) / 2
                   if (position < 0) {
                       page.setTranslationX(horzMargin - vertMargin / 2)
                   } else {
                       page.setTranslationX(-horzMargin + vertMargin / 2)
                   }

                   // Scale the page down (between MIN_SCALE and 1)
                   page.scaleX = scaleFactor
                   page.scaleY = scaleFactor

                   // Fade the page relative to its size.
                   page.alpha = MIN_ALPHA +
                           (scaleFactor - MIN_SCALE) /
                           (1 - MIN_SCALE) * (1 - MIN_ALPHA)
               }
               else -> { // (1,+Infinity]
                   // This page is way off-screen to the right.
                   page.alpha = 0f
               }
           }

       }

   }
}