// `cronstrue/locales/*` ships JS only, no type declarations. We import the
// pt-BR locale for its side effect (registering the locale); this ambient
// declaration keeps `noUncheckedSideEffectImports` satisfied.
declare module "cronstrue/locales/pt_BR";
