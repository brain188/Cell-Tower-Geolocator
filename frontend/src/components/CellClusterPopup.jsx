import React from "react";

const CellClusterPopup = ({ cells }) => (
  <div>
    <h4>Cells in this area</h4>
    <ul style={{ maxHeight: "120px", overflowY: "auto" }}>
      {cells.map(cell => (
        <li key={cell.id}>
          LAC: {cell.lac}, CID: {cell.cellId}
        </li>
      ))}
    </ul>
  </div>
);

export default CellClusterPopup;
