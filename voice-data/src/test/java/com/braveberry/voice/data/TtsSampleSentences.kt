package com.braveberry.voice.data

/**
 * 서비스 시나리오(도착 알림, 안전벨트, 소지품, 가이드 안내)의 실제 언어별 문장.
 * 합성 엔진은 텍스트로 언어를 판단하므로 languageCode와 같은 언어의 문장을 사용해야 함.
 * 일본어·중국어는 전각 문장부호(。！？、，)를 그대로 유지함.
 */
internal object TtsSampleSentences {
    object Japanese {
        const val LANGUAGE = "ja-JP"
        const val SEATBELT = "シートベルトを着用してください！"
        const val ARRIVAL = "まもなく目的地に到着します。お忘れ物がないかご確認ください。"
        const val GUIDE_OFFER = "観光ガイドをお聞きになりますか？"
    }

    object Chinese {
        const val LANGUAGE = "cmn-CN"
        const val SEATBELT = "请系好安全带！"
        const val ARRIVAL = "即将到达目的地。请检查您的随身物品。"
        const val GUIDE_OFFER = "您好，请问您需要观光导游服务吗？"
    }

    object English {
        const val LANGUAGE = "en-US"
        const val SEATBELT = "Please fasten your seatbelt!"
        const val ARRIVAL = "We will arrive at your destination shortly. Please check your belongings."
        const val GUIDE_OFFER = "Would you like to hear a sightseeing guide?"
    }
}
