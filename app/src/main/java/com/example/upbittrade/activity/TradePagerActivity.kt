package com.example.upbittrade.activity

import android.opengl.ETC1.getWidth
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.upbittrade.R
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.DefaultViewModel
import com.example.upbittrade.model.TradeViewModel
import com.example.upbittrade.utils.PreferenceUtil

@Suppress("PrivatePropertyName")
class TradePagerActivity : FragmentActivity() {

    object KeyObject {
        var accessKey : String? = null
        var secretKey : String? = null
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
        SEARCH_ORDER_INFO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_pager)

        val preferenceUtil = PreferenceUtil(application)
        KeyObject.accessKey = preferenceUtil.getString(preferenceUtil.ACCESS_KEY, "")
        KeyObject.secretKey = preferenceUtil.getString(preferenceUtil.SECRET_KEY, "")

        val viewModel: TradeViewModel = ViewModelProvider(this).get(TradeViewModel::class.java)
        val viewPager = findViewById<ViewPager2>(R.id.pager)
        viewPager.adapter = ScreenSlidePagerAdapter(this, viewModel)
        viewPager.setPageTransformer(ZoomOutPageTransformer())
    }

    class ScreenSlidePagerAdapter(activity: TradePagerActivity, private val model: TradeViewModel): FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return 1
        }

        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> TradeFragment(model!!)
                else -> TradeFragment(model!!)
            }

        }

    }

   class ZoomOutPageTransformer: ViewPager2.PageTransformer {
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

    companion object {
        const val TAG = "DefaultViewModel"
    }
}