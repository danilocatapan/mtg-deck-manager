import React from 'react'
import logo from '../assets/logo.png'

export default function Logo() {
  return (
    <img
      className="brand-logo"
      src={logo}
      alt="MTG Deck Manager"
    />
  )
}
