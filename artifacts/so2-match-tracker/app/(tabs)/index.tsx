import { Feather } from '@expo/vector-icons';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useColors } from '@/hooks/useColors';

const included = [
  'Запись матча и редактирование',
  'Локальная база Room',
  'История, поиск и фильтры',
  'Статистика и графики',
  'Резервная копия JSON',
];

export default function BuildInfoScreen() {
  const colors = useColors();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.brandRow}>
          <View style={[styles.iconFrame, { backgroundColor: colors.accent }]}>
            <Feather name="crosshair" size={24} color={colors.primary} />
          </View>
          <View>
            <Text style={[styles.kicker, { color: colors.primary }]}>
              ANDROID · KOTLIN
            </Text>
            <Text style={[styles.title, { color: colors.foreground }]}>
              SO2 Match Tracker
            </Text>
          </View>
        </View>

        <View
          style={[
            styles.statusCard,
            { backgroundColor: colors.card, borderColor: colors.border },
          ]}
        >
          <View style={styles.statusHeading}>
            <Feather name="smartphone" size={18} color={colors.primary} />
            <Text style={[styles.cardTitle, { color: colors.foreground }]}>
              Нативный проект готов к сборке
            </Text>
          </View>
          <Text style={[styles.body, { color: colors.mutedForeground }]}>
            Основное приложение написано на Kotlin и Jetpack Compose. Матчи
            сохраняются на устройстве в Room; аккаунт и подключение к интернету
            не нужны.
          </Text>
          <View
            style={[
              styles.notice,
              { backgroundColor: colors.accent, borderColor: colors.border },
            ]}
          >
            <Feather name="info" size={16} color={colors.primary} />
            <Text style={[styles.noticeText, { color: colors.foreground }]}>
              APK не собран: в текущей среде нет Android SDK и JDK. Полная
              инструкция находится в README Android-проекта.
            </Text>
          </View>
        </View>

        <Text style={[styles.sectionTitle, { color: colors.foreground }]}>
          В приложении
        </Text>
        <View
          style={[
            styles.featureCard,
            { backgroundColor: colors.card, borderColor: colors.border },
          ]}
        >
          {included.map((item, index) => (
            <View
              key={item}
              style={[
                styles.featureRow,
                index < included.length - 1 && {
                  borderBottomColor: colors.border,
                  borderBottomWidth: StyleSheet.hairlineWidth,
                },
              ]}
            >
              <Feather name="check" size={16} color={colors.primary} />
              <Text style={[styles.featureText, { color: colors.foreground }]}>
                {item}
              </Text>
            </View>
          ))}
        </View>

        <View style={styles.footer}>
          <Text style={[styles.footerText, { color: colors.mutedForeground }]}>
            Исходный Android-проект: папка android внутри SO2 Match Tracker.
          </Text>
          <Text style={[styles.footerText, { color: colors.mutedForeground }]}>
            Минимальная версия: Android 14 · целевая сборка: Android 16.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  content: { padding: 20, paddingBottom: 32, gap: 18 },
  brandRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingTop: 8 },
  iconFrame: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 16,
  },
  kicker: { fontSize: 10, fontWeight: '700', letterSpacing: 1.4 },
  title: { fontSize: 22, fontWeight: '700', marginTop: 2 },
  statusCard: {
    borderWidth: 1,
    borderRadius: 20,
    padding: 17,
    gap: 12,
  },
  statusHeading: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  cardTitle: { fontSize: 16, fontWeight: '700', flexShrink: 1 },
  body: { fontSize: 14, lineHeight: 21 },
  notice: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 9,
    padding: 12,
    borderWidth: 1,
    borderRadius: 13,
  },
  noticeText: { flex: 1, fontSize: 13, lineHeight: 19 },
  sectionTitle: { fontSize: 18, fontWeight: '700', marginBottom: -8 },
  featureCard: { borderWidth: 1, borderRadius: 18, paddingHorizontal: 14 },
  featureRow: {
    minHeight: 48,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  featureText: { fontSize: 14, fontWeight: '500' },
  footer: { gap: 5, paddingHorizontal: 2 },
  footerText: { fontSize: 12, lineHeight: 17 },
});
