package team.burkart.zero.packet

class FeatureConfigPacket(private val requestOnly: Boolean = false) : BasePacket()
{
	companion object {
		val packetType : Short = 32
		val packetTypeSet : Short = 33
		val hashSize : Int = 32
	}
	override fun getPacketType(): Short {
		return if(requestOnly) packetType else packetTypeSet
	}

	class Feature() {
		companion object {
			enum class FeatureCode(val value: Int) {
				None(0), Charger3KWOE(1), Charger3KWACC(2), Charger6KWACC(3), HeatedGrips(4),
				LEDBlinkers(5), DcDc500W(6), Feature07(7), FasterCharge10(8), Charger6KWFlat(9),
				ParkingMode(10), PerformanceBoost(11), ExtraBattery10(12), ExtraRange10(13), ChargeTank(14),
				Unknown(15);
				companion object {
					fun fromValue(value: Int) : FeatureCode {
						if ((value < FeatureCode.None.value) or (value > FeatureCode.ChargeTank.value)) {return Unknown;}
						return FeatureCode.entries[value.toInt()]
					}
				}
			}
			enum class UnavailableCause(val value: Byte) {
				FirmwareMBB(1), ChargerMissing(2), FirmwareDash(4), BatteryMissing(8),
				Unsupported(16), OtherFeature(32), FirmwareCharger(64);
				companion object {
					fun toString(value: Byte) : String {
						var result : Array<String> = arrayOf()
						UnavailableCause.entries.forEach { it -> if (value.toInt().and(it.value.toInt()) != 0) result += it.name }
						return result.joinToString("|")
					}
				}
			}

			const val dataSize = 6
		}
		constructor(data: ByteArray) : this() {
			if (data.size == dataSize) {
				code = FeatureCode.fromValue(number(data.sliceArray(0..3)))
				enabled = data[4] != 0.toByte()
				unavailableCause = data[5]
			}
		}

		override fun toString(): String {
			return "${code} ${if (enabled) "enabled" else "disabled"} / ${UnavailableCause.toString(unavailableCause)}"
		}

		var code: FeatureCode = FeatureCode.Unknown
		var enabled: Boolean = false
		var unavailableCause: Byte = 0
	}

	override fun getPayload(): ByteArray {
		var result = ByteArray(0)
		if (requestOnly) {return result;}
		result += number(featureToSet.value)

		var hashData = hash.toByteArray()
		if (hashData.size < hashSize) {
			hashData += ByteArray(hashSize - hashData.size)
		} else if (hashData.size > hashSize) {
			hashData = hashData.sliceArray(0..< hashSize)
		}
		result += hashData.size.toByte()
		result += hashData

		return result;
	}

	var features: Array<Feature> = arrayOf()

	var featureToSet: Feature.Companion.FeatureCode = Feature.Companion.FeatureCode.None
	var hash : String = ""


	constructor(data: ByteArray) : this() {
		val featureCount = number(data.sliceArray(0..3))
		val featureData = data.sliceArray(4 ..< data.size)
		for (i in 0..< featureCount ) {
			features += Feature(featureData.sliceArray(i * Feature.dataSize..<(i + 1) * Feature.dataSize))
		}
	}

	override fun toString() : String {
		return if (requestOnly) "Request" else features.joinToString("\n")
	}
}