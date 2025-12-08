package com.example.service

import com.example.model.Crypto

class BtcPriceService {

    fun getBtcPrice(): Crypto {
        // In a real application, this method would fetch the current Bitcoin price from an API.
        // Here, we return a hardcoded value for demonstration purposes.
        return Crypto(pris = "30000 USD")
    }

}