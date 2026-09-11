/** `<datalist>` of known X-External-System values; pair with `<input list={id}>`. */
export function SystemsDatalist({
  id,
  systems,
}: {
  id: string;
  systems: string[];
}) {
  return (
    <datalist id={id}>
      {systems.map((s) => (
        <option key={s} value={s} />
      ))}
    </datalist>
  );
}
